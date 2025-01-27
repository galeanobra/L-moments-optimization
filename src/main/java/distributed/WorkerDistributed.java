package distributed;

import org.uma.jmetal.parallel.asynchronous.task.ParallelTask;
import org.uma.jmetal.problem.Problem;
import util.JMetalLogger;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class WorkerDistributed<T extends ParallelTask<?>> {
    protected String ip;
    protected int port;
    protected Socket socket;

    protected String myIp;

    public WorkerDistributed(String ip, int port) {
        this.ip = ip;
        this.port = port;
        try {
            this.myIp = InetAddress.getLocalHost().toString();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        connectsToServer();
    }

    protected void connectsToServer() {
        try {
            JMetalLogger.logger.info("Worker trying to connect to the server");
            socket = new Socket(ip, port);
        } catch (IOException e) {
            JMetalLogger.logger.info("Worker (" + myIp + ") error: can't establish connection with server\n" + e);
        }
    }

    public void run() {
        try (ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {

            if (socket == null || socket.isClosed()) {
                JMetalLogger.logger.info("Worker (" + myIp + ") cannot run without a connection.");
                return;
            }

            Problem problem = (Problem) input.readObject();
            output.writeObject(myIp);
            output.flush();

            JMetalLogger.logger.info("Worker " + myIp + " starting");
            JMetalLogger.logger.info("Receiving tasks");

            while (!socket.isClosed()) {
                try {
                    Object receivedTask = input.readObject();

                    if (!(receivedTask instanceof ParallelTask)) {
                        JMetalLogger.logger.info("Worker " + myIp + " received invalid task.");
                        break;
                    }

                    @SuppressWarnings("unchecked")
                    T task = (T) receivedTask;

                    problem.evaluate(task.getContents());

                    output.writeObject(task);
                    output.flush();
                    output.reset();

                    JMetalLogger.logger.info("Worker " + myIp + " task sent back.");

                } catch (EOFException e) {
                    JMetalLogger.logger.info("Worker " + myIp + " stopping by master (EOF detected)");
                    break;
                } catch (IOException | ClassNotFoundException e) {
                    JMetalLogger.logger.info("Worker " + myIp + " encountered an error:\n" + e.getMessage());
                    break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            JMetalLogger.logger.info("Worker " + myIp + " encountered a critical error: " + e.getMessage());
        } finally {
            JMetalLogger.logger.info("Worker " + myIp + " cleaning up resources.");
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException ignored) {
            }
            JMetalLogger.logger.info("Worker " + myIp + " stops.");
        }
    }
}
