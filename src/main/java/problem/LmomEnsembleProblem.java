package problem;

import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.binarysolution.BinarySolution;
import org.uma.jmetal.solution.binarysolution.impl.DefaultBinarySolution;
import org.uma.jmetal.solution.compositesolution.CompositeSolution;
import org.uma.jmetal.solution.integersolution.IntegerSolution;
import org.uma.jmetal.solution.integersolution.impl.DefaultIntegerSolution;
import org.uma.jmetal.util.binarySet.BinarySet;
import org.uma.jmetal.util.bounds.Bounds;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static java.lang.Math.floor;
import static java.lang.Math.sqrt;

public class LmomEnsembleProblem implements Problem<CompositeSolution> {

    protected int numberOfBitsFeatures;
    protected int numberOfBitsEnsemble;
    protected int numberOfIntegers;
    protected int numberOfDoubles;
    protected String jsonFile;

    protected List<Bounds<Integer>> integerBounds;
    protected List<Bounds<Double>> doubleBounds;

    public LmomEnsembleProblem(int numberOfBitsFeatures, int numberOfBitsEnsemble, int numberOfIntegers, int numberOfDoubles, String jsonFile) {
        this.numberOfBitsFeatures = numberOfBitsFeatures;
        this.numberOfBitsEnsemble = numberOfBitsEnsemble;
        this.jsonFile = jsonFile;

        integerBounds = new ArrayList<>(numberOfIntegers);

        integerBounds.add(Bounds.create(10, 200));  // n
        integerBounds.add(Bounds.create(2, 5));  // svc_poly_degree
        integerBounds.add(Bounds.create(5, (int) floor(sqrt(floor((191694.0 / integerBounds.get(0).getUpperBound()) * 0.8 * 0.8) / 2.0))));  // n_neighbors

        if (numberOfDoubles > 0) {
            doubleBounds = new ArrayList<>(numberOfDoubles);
            doubleBounds.add(Bounds.create(0.001, 1.000));  // svcnu_nu
        }
    }

    @Override
    public int numberOfVariables() {
        return 2 + integerBounds.size() + doubleBounds.size();
    }

    @Override
    public int numberOfObjectives() {
        return 3;
    }

    @Override
    public int numberOfConstraints() {
        return 0;
    }

    @Override
    public String name() {
        return "LmomEnsembleProblem";
    }

    @Override
    public CompositeSolution evaluate(CompositeSolution compositeSolution) {

        BinarySet binarySetFeatures = (BinarySet) compositeSolution.variables().get(0).variables().get(0);
        BinarySet binarySetEnsemble = (BinarySet) compositeSolution.variables().get(1).variables().get(0);
        List<Integer> integers = (List<Integer>) compositeSolution.variables().get(2).variables();

        System.out.println(formatVariables(binarySetFeatures, binarySetEnsemble, integers));

        if (binarySetFeatures.cardinality() > 0) {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "python", "lmoments/src", formatVariables(binarySetFeatures, binarySetEnsemble, integers)
            );
            processBuilder.redirectErrorStream(true);

            Process p = null;

            try {
                p = processBuilder.start();
                final Process process = p;

                ExecutorService executor = Executors.newSingleThreadExecutor();
                Future<List<String>> outputFuture = executor.submit(() ->
                        new BufferedReader(new InputStreamReader(process.getInputStream())).lines().toList()
                );

                try {
                    List<String> result = outputFuture.get(60, TimeUnit.MINUTES);

                    int exitCode = p.waitFor();
                    executor.shutdown();

                    if (exitCode == 0) {
                        double accuracy;

                        try {
                            accuracy = Double.parseDouble(result.get(result.size() - 1));
                        } catch (Exception e) {
                            System.err.println("Error parsing output: " + Arrays.toString(result.toArray()));
                            accuracy = 0.0;
                        }

                        if (accuracy >= 0.0 && accuracy <= 1.0) {
                            compositeSolution.objectives()[0] = integers.get(0);
                            compositeSolution.objectives()[1] = binarySetFeatures.cardinality();
                            compositeSolution.objectives()[2] = -accuracy;
                        } else {
                            nullObjectives(compositeSolution);
                        }
                    } else {
                        System.err.println("Python process failed with exit code " + exitCode);
                        nullObjectives(compositeSolution);
                    }

                } catch (TimeoutException e) {
                    System.err.println("Python process timed out after 15 minutes.");
                    outputFuture.cancel(true);
                    nullObjectives(compositeSolution);
                } finally {
                    executor.shutdownNow();
                }

            } catch (IOException | InterruptedException | ExecutionException e) {
                System.err.println("Python process failed: " + e.getMessage());
                e.printStackTrace();
                nullObjectives(compositeSolution);
            } finally {
                try {
                    p.destroyForcibly();
                } catch (Exception ignored) {
                }
            }


        } else nullObjectives(compositeSolution);

        return compositeSolution;
    }

    @Override
    public CompositeSolution createSolution() {
        BinarySolution binarySolutionFeatures = new DefaultBinarySolution(List.of(numberOfBitsFeatures), numberOfObjectives(), numberOfConstraints());
        BinarySolution binarySolutionEnsemble = new DefaultBinarySolution(List.of(numberOfBitsEnsemble), numberOfObjectives(), numberOfConstraints());
        IntegerSolution integerSolution = new DefaultIntegerSolution(integerBounds, numberOfObjectives(), numberOfConstraints());

        return new CompositeSolution(Arrays.asList(binarySolutionFeatures, binarySolutionEnsemble, integerSolution));
    }

    public void nullObjectives(CompositeSolution compositeSolution) {
        compositeSolution.objectives()[0] = integerBounds.get(0).getUpperBound();
        compositeSolution.objectives()[1] = numberOfBitsFeatures;
        compositeSolution.objectives()[2] = 0.0;
    }

    public String formatVariables(BinarySet binarySetFeatures, BinarySet binarySetEnsemble, List<Integer> integers, List<Double> doubles) {
        return binarySetFeatures.toString() + " - " + binarySetEnsemble.toString() + " - " + integers.stream().map(String::valueOf).collect(Collectors.joining(",")) + " - " + doubles.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    public String formatVariables(BinarySet binarySetFeatures, BinarySet binarySetEnsemble, List<Integer> integers) {
        return binarySetFeatures.toString() + " - " + binarySetEnsemble.toString() + " - " + integers.stream().map(String::valueOf).collect(Collectors.joining(",")) + " = " + jsonFile;
    }
}
