import distributed.WorkerDistributed;

public class Worker {
    public static void main(String[] args) {
        new WorkerDistributed<>(args[0], Integer.parseInt(args[1])).run();
    }
}