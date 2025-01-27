import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import distributed.algorithms.AsynchronousDistributedNSGAII;
import operator.BinaryTwoPointCrossover;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.operator.crossover.impl.CompositeCrossover;
import org.uma.jmetal.operator.crossover.impl.IntegerSBXCrossover;
import org.uma.jmetal.operator.mutation.impl.BitFlipMutation;
import org.uma.jmetal.operator.mutation.impl.CompositeMutation;
import org.uma.jmetal.operator.mutation.impl.IntegerPolynomialMutation;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.compositesolution.CompositeSolution;
import org.uma.jmetal.util.ConstraintHandling;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import problem.LmomEnsembleProblem;
import util.JMetalLogger;
import util.JSONCounter;
import util.WriteSolutionsToCSVObserver;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class NSGAIIMain {
    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);

        int populationSize = Integer.parseInt(args[1]);
        int maxEvaluations = Integer.parseInt(args[2]);
        String jsonFile = "lmoments/conf_default/" + args[3];

        int numberOfBitsFeatures = 0;
        int numberOfIntegers = 0;

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(new File(jsonFile));
            JSONCounter jsonCounter = countVariables(rootNode);

            numberOfBitsFeatures = jsonCounter.numberOfBits * 3;
            numberOfIntegers = jsonCounter.numberOfIntegers;
        } catch (IOException e) {
            JMetalLogger.logger.severe("Problem reading json file: " + jsonFile);
        } finally {
            boolean error = false;
            if (numberOfBitsFeatures == 0) {
                JMetalLogger.logger.severe("No features to select!");
                error = true;
            }
            if (numberOfIntegers == 0) {
                JMetalLogger.logger.severe("No integer variables!");
                error = true;
            }

            if (error)
                System.exit(1);
        }

        int numberOfBitsEnsemble = numberOfIntegers - 3;
        numberOfIntegers = 3;

        JMetalLogger.logger.info("Problem variables: " + numberOfBitsFeatures + " bits (features), 7 bits (ensemble parameters), " + numberOfBitsEnsemble + " integers"); //, " + numberOfDoubles + " doubles");

        int numberOfConstraints = 0;

        double binaryCrossoverProbabilityFeatures = 0.9;
        double binaryCrossoverProbabilityEnsemble = 0.9;
        double integerCrossoverProbability = 0.9;

        double binaryMutationProbabilityFeatures = 1.0 / numberOfBitsFeatures;
        double binaryMutationProbabilityEnsemble = 1.0 / numberOfBitsEnsemble;
        double integerMutationProbability = 1.0 / numberOfIntegers;

        Problem<CompositeSolution> problem = new LmomEnsembleProblem(numberOfBitsFeatures, numberOfBitsEnsemble, numberOfIntegers, 0, jsonFile);

        CompositeCrossover crossover = new CompositeCrossover(Arrays.asList(
                new BinaryTwoPointCrossover(binaryCrossoverProbabilityFeatures),
                new BinaryTwoPointCrossover(binaryCrossoverProbabilityEnsemble),
                new IntegerSBXCrossover(integerCrossoverProbability, 20.0)));
        CompositeMutation mutation = new CompositeMutation(Arrays.asList(
                new BitFlipMutation<>(binaryMutationProbabilityFeatures),
                new BitFlipMutation<>(binaryMutationProbabilityEnsemble),
                new IntegerPolynomialMutation(integerMutationProbability, 20.0)));

        Termination termination = new TerminationByEvaluations(maxEvaluations);
        WriteSolutionsToCSVObserver evaluationObserver = new WriteSolutionsToCSVObserver(100, "traces");

        AsynchronousDistributedNSGAII<CompositeSolution> algorithm = new AsynchronousDistributedNSGAII<>(port, problem, populationSize, crossover, mutation, termination);
        algorithm.observable().register(evaluationObserver);
        algorithm.run();

        List<CompositeSolution> population = algorithm.getResult();

        JMetalLogger.logger.info("Solutions saved at FUN.csv and VAR.csv");

        new SolutionListOutput(population)
                .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
                .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
                .print();

        if (numberOfConstraints > 0) {
            new SolutionListOutput(population.stream().filter(ConstraintHandling::isFeasible).collect(Collectors.toList()))
                    .setVarFileOutputContext(new DefaultFileOutputContext("VAR_feasible.csv", ","))
                    .setFunFileOutputContext(new DefaultFileOutputContext("FUN_feasible.csv", ",")).print();
        }

        System.exit(0);
    }

    public static JSONCounter countVariables(JsonNode node) {
        int numberOfFeatures = 0;
        int numberOfIntegers = 0;
        int numberOfDoubles = 0;

        if (node.isObject()) {
            JsonNode featuresNode = node.get("features");
            if (featuresNode != null && featuresNode.isArray()) {
                numberOfFeatures = featuresNode.size();
            }

            for (JsonNode child : node) {
                JSONCounter childStats = countVariables(child);
                numberOfFeatures += childStats.numberOfBits;
                numberOfIntegers += childStats.numberOfIntegers;
                numberOfDoubles += childStats.numberOfDoubles;
            }
        } else if (node.isArray()) {
            for (JsonNode element : node) {
                JSONCounter elementStats = countVariables(element);
                numberOfFeatures += elementStats.numberOfBits;
                numberOfIntegers += elementStats.numberOfIntegers;
                numberOfDoubles += elementStats.numberOfDoubles;
            }
        } else if (node.isInt()) {
            numberOfIntegers++;
        } else if (node.isDouble()) {
            numberOfDoubles++;
        }

        return new JSONCounter(numberOfFeatures, numberOfIntegers, numberOfDoubles);
    }
}
