package distributed.algorithms;

import distributed.AsynchronousDistributedEvolutionaryAlgorithm;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.solution.compositesolution.CompositeSolution;
import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;
import org.uma.jmetal.util.comparator.dominanceComparator.impl.DefaultDominanceComparator;

public class AsynchronousDistributedNSGAII<S extends Solution<?>> extends AsynchronousDistributedEvolutionaryAlgorithm<CompositeSolution> {

    public AsynchronousDistributedNSGAII(int port, Problem<CompositeSolution> problem, int populationSize, CrossoverOperator<CompositeSolution> crossover, MutationOperator<CompositeSolution> mutation, Termination termination) {
        super(port, problem, populationSize, crossover, mutation, new BinaryTournamentSelection<>(new RankingAndCrowdingDistanceComparator<>()), new DefaultDominanceComparator<>(), termination);
        // new DominanceWithConstraintsComparator<>(new OverallConstraintViolationDegreeComparator<>())
        //new RankingAndDensityEstimatorReplacement<>(new MergeNonDominatedSortRanking<>(), new CrowdingDistanceDensityEstimator<>(), Replacement.RemovalPolicy.ONE_SHOT)
        waitForWorkers();
    }
}
