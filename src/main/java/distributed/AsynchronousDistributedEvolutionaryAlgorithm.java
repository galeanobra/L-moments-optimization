package distributed;

import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.operator.selection.impl.RankingAndCrowdingSelection;
import org.uma.jmetal.parallel.asynchronous.task.ParallelTask;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.archive.Archive;
import org.uma.jmetal.util.archive.impl.BestSolutionsArchive;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.comparator.dominanceComparator.DominanceComparator;
import org.uma.jmetal.util.observable.Observable;
import org.uma.jmetal.util.observable.ObservableEntity;
import org.uma.jmetal.util.observable.impl.DefaultObservable;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;
import util.JMetalLogger;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class AsynchronousDistributedEvolutionaryAlgorithm<S extends Solution<?>> extends MasterDistributed<ParallelTask<S>, List<S>> implements ObservableEntity<Map<String, Object>> {
    protected final Problem<S> problem;
    protected CrossoverOperator<S> crossover;
    protected final MutationOperator<S> mutation;
    protected final SelectionOperator<List<S>, S> selection;
    protected DominanceComparator<S> dominanceComparator;
    protected final Termination termination;
    protected final int populationSize;
    protected final Map<String, Object> attributes;
    protected final Observable<Map<String, Object>> observable;
    protected List<S> population;
    protected int evaluations = 0;
    protected long initTime;
    protected int idCounter;
    protected String FUNFile;
    protected String VARFile;
    protected List<ParallelTask<S>> initialTaskListFromFUN;
    boolean firstPopFromFUN;
    protected Archive<S> archive;


    public AsynchronousDistributedEvolutionaryAlgorithm(int port, Problem<S> problem, int populationSize, CrossoverOperator<S> crossover, MutationOperator<S> mutation, SelectionOperator<List<S>, S> selection, DominanceComparator<S> dominanceComparator, Termination termination) {
        super(port, problem);
        this.problem = problem;
        this.crossover = crossover;
        this.mutation = mutation;
        this.populationSize = populationSize;
        this.termination = termination;
        this.selection = selection;
        this.dominanceComparator = dominanceComparator;
        this.population = new ArrayList<>();

        attributes = new HashMap<>();
        observable = new DefaultObservable<>("Observable");

        idCounter = 0;

        archive = new BestSolutionsArchive<>(new NonDominatedSolutionListArchive<>(), Integer.MAX_VALUE);
    }

    protected void waitForWorkers() {
        new Thread(() -> {
            JMetalLogger.logger.info("Waiting for workers");
            while (true) {
                try {
                    acceptConnection();
                } catch (IOException e) {
                    JMetalLogger.logger.info("ERROR in server socket:\n\t" + e);
                }
            }
        }).start();
    }

    public void acceptConnection() throws IOException {
        Socket socket = serverSocket.accept();
        Thread t = new Thread(new WorkerTalker(socket));
        workerThreads.add(t);
        t.start();
    }

    public int createTaskIdentifier() {
        int id = idCounter;
        idCounter++;
        return id;
    }

    @Override
    public void initProgress() {
        attributes.put("EVALUATIONS", evaluations);
        attributes.put("POPULATION", population);
        attributes.put("COMPUTING_TIME", System.currentTimeMillis() - initTime);

        observable.setChanged();
        observable.notifyObservers(attributes);
    }

    @Override
    public void updateProgress() {
        attributes.put("EVALUATIONS", evaluations);
        attributes.put("POPULATION", population);
        attributes.put("COMPUTING_TIME", System.currentTimeMillis() - initTime);
        attributes.put("BEST_SOLUTION", population.get(0));

        observable.setChanged();
        observable.notifyObservers(attributes);
    }

    public void setVARFile(String VARFile) {
        this.VARFile = VARFile;
    }

    public void setFUNFile(String FUNFile) {
        this.FUNFile = FUNFile;
    }

    @Override
    public List<ParallelTask<S>> createInitialTasks() {
        List<S> initialPopulation = new ArrayList<>();
        List<ParallelTask<S>> initialTaskList = new ArrayList<>();

        IntStream.range(0, populationSize).forEach(i -> initialPopulation.add(problem.createSolution()));
        initialPopulation.forEach(solution -> initialTaskList.add(ParallelTask.create(createTaskIdentifier(), solution)));

        return initialTaskList;
    }

    @Override
    public void submitInitialTasks(List<ParallelTask<S>> initialTasks) {
        if (FUNFile == null)
            initialTasks.forEach(this::submitTask);
    }

    @Override
    public ParallelTask<S> waitForComputedTask() {
        ParallelTask<S> evaluatedTask = null;
        if (!firstPopFromFUN) {
            try {
                evaluatedTask = completedTaskQueue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            if (!initialTaskListFromFUN.isEmpty()) {
                evaluatedTask = initialTaskListFromFUN.remove(0);
            } else {
                firstPopFromFUN = false;
                try {
                    evaluatedTask = completedTaskQueue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }

        return evaluatedTask;
    }

    @Override
    public void processComputedTask(ParallelTask<S> task) {

        evaluations++;

        archive.add((S) task.getContents().copy());


        if (population.size() < populationSize) {
            population.add(task.getContents());
        } else {
            synchronized (population) {
                List<S> offspringPopulation = new ArrayList<>(population);
                offspringPopulation.add(task.getContents());
                population = new RankingAndCrowdingSelection<>(populationSize, dominanceComparator).execute(offspringPopulation);
            }
        }
    }

    @Override
    public ParallelTask<S> createNewTask() {
        synchronized (population) {
            if (population.size() > 2) {
                List<S> parents = new ArrayList<>(2);
                parents.add(selection.execute(population));
                parents.add(selection.execute(population));

                List<S> offspring = crossover.execute(parents);

                S sol0 = (S) offspring.get(0).copy();
                S sol1 = (S) offspring.get(1).copy();

                mutation.execute(sol0);
                mutation.execute(sol1);

                if (JMetalRandom.getInstance().nextInt(0, 1) == 0) {
                    pendingTaskQueue.add(ParallelTask.create(createTaskIdentifier(), sol1));
                    return ParallelTask.create(createTaskIdentifier(), sol0);
                } else {
                    pendingTaskQueue.add(ParallelTask.create(createTaskIdentifier(), sol0));
                    return ParallelTask.create(createTaskIdentifier(), sol1);
                }
            } else return ParallelTask.create(createTaskIdentifier(), problem.createSolution());
        }
    }

    @Override
    public boolean stoppingConditionIsNotMet() {
        return !termination.isMet(attributes);
    }

    @Override
    public void run() {
        initTime = System.currentTimeMillis();
        super.run();
    }

    @Override
    public List<S> getResult() {
        return SolutionListUtils.distanceBasedSubsetSelection(archive.solutions(), populationSize);
    }

    @Override
    public Observable<Map<String, Object>> observable() {
        return observable;
    }
}
