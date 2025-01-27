package distributed;

import org.uma.jmetal.parallel.asynchronous.task.ParallelTask;
import org.uma.jmetal.problem.Problem;
import util.JMetalLogger;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public abstract class MasterDistributed<T extends ParallelTask<?>, R> implements AsynchronousParallelAlgorithm<T, R> {

    protected BlockingQueue<T> completedTaskQueue;
    protected BlockingQueue<T> pendingTaskQueue;
    protected BlockingQueue<Thread> workerThreads;
    protected ServerSocket serverSocket;
    protected int numWorkers;
    protected int port;
    protected Problem problem;

    public MasterDistributed(int port, Problem problem) {
        completedTaskQueue = new LinkedBlockingQueue<>();
        pendingTaskQueue = new LinkedBlockingQueue<>();
        workerThreads = new LinkedBlockingQueue<>();
        numWorkers = 0;

        this.port = port;
        this.problem = problem;

        openSocket();
    }

    public void openSocket() {
        try {
            serverSocket = new ServerSocket(port);
            port = serverSocket.getLocalPort();
            String ip = null;

            try {
                DatagramSocket socket = new DatagramSocket();
                socket.connect(InetAddress.getByName("8.8.8.8"), 65431);
                ip = socket.getLocalAddress().getHostAddress();

                PrintWriter out = new PrintWriter("serverIP.dat");
                out.print(ip);
                out.close();
                socket.close();
            } catch (Exception ignored) {
                try {
                    ProcessBuilder pb = new ProcessBuilder("bash", "-c", "hostname -I | awk '{print $1}' > serverIP.dat");
                    pb.redirectErrorStream(true);

                    Process p = pb.start();
                    List<String> lines = (new BufferedReader(new InputStreamReader(p.getInputStream()))).lines().collect(Collectors.toList());
                    Scanner sc = new Scanner(new File("serverIP.dat"));
                    ip = sc.nextLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            JMetalLogger.logger.info("Server listening on port " + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void submitInitialTasks(List<T> initialTasks) {
        initialTasks.forEach(this::submitTask);
    }

    @Override
    public T waitForComputedTask() {
        T evaluatedTask = null;
        try {
            evaluatedTask = completedTaskQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return evaluatedTask;
    }

    @Override
    public abstract void processComputedTask(T task);

    @Override
    public void submitTask(T task) {
        pendingTaskQueue.add(task);
    }

    @Override
    public abstract T createNewTask();

    @Override
    public T getPendingTask() throws InterruptedException {
        if (pendingTaskQueue.size() > 0) return pendingTaskQueue.take();
        return null;
    }

    @Override
    public abstract boolean stoppingConditionIsNotMet();

    @Override
    public int numIdleWorkers() {
        return numWorkers - pendingTaskQueue.size();
    }

    public BlockingQueue<T> getCompletedTaskQueue() {
        return completedTaskQueue;
    }

    public BlockingQueue<T> getPendingTaskQueue() {
        return pendingTaskQueue;
    }

    public class WorkerTalker implements Runnable {

        private final Socket socket;

        private String ip;

        public WorkerTalker(Socket socket) {
            this.socket = socket;
            this.ip = "";
            numWorkers++;
        }

        @Override
        public void run() {
            try {
                ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream input = new ObjectInputStream(socket.getInputStream());

                output.writeObject(problem);
                output.flush();
                output.reset();
                try {
                    ip = (String) input.readObject();
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }

                JMetalLogger.logger.info("Worker " + ip + " connected");
                long beforeUsedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

                int tries = 0;

                // Send task until the stopping condition is met
                while (stoppingConditionIsNotMet()) {
                    try {
                        T taskToCompute = getPendingTask();

                        if (taskToCompute == null) taskToCompute = createNewTask();

                        long startTime = System.nanoTime();
                        output.writeObject(taskToCompute);
                        output.flush();
                        output.reset();
                        long endTime = System.nanoTime();

                        try {
                            startTime = System.nanoTime();
                            T computedTask = (T) input.readObject();
                            endTime = System.nanoTime();

                            JMetalLogger.logger.info("Waiting evaluation " + (endTime - startTime) / 1_000_000 + "ms.");

                            completedTaskQueue.add(computedTask);

                            Thread.sleep(5);

                        } catch (EOFException e) {  // If the worker fails during the evaluation
                            JMetalLogger.logger.info("Worker " + ip + " down");
                            pendingTaskQueue.add(taskToCompute);
                            numWorkers--;
                            JMetalLogger.logger.info("Active workers: " + numWorkers);
                            Thread.currentThread().interrupt();
                        } catch (ClassNotFoundException ignored) {
                        }
                    } catch (InterruptedException ignored) {
                    } catch (SocketException ignored) {
                        output.reset();
                    }
                }

                output.write(-1);
                socket.close();
            } catch (IOException ignored) {
                try {
                    socket.close();
                } catch (IOException e) {

                }
            } finally {
                if (stoppingConditionIsNotMet()) JMetalLogger.logger.info("Worker " + ip + " disconnected");
                numWorkers--;
            }
        }
    }
}
