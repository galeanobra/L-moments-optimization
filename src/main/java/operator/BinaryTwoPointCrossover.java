package operator;

import org.apache.commons.lang3.ArrayUtils;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.solution.binarysolution.BinarySolution;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

import java.util.ArrayList;
import java.util.List;

public class BinaryTwoPointCrossover implements CrossoverOperator<BinarySolution> {

    protected final double probability;

    protected final JMetalRandom randomNumberGenerator = JMetalRandom.getInstance();

    public BinaryTwoPointCrossover(double probability) {
        if (probability < 0.0) throw new JMetalException("Probability can't be negative");
        this.probability = probability;
    }

    @Override
    public double crossoverProbability() {
        return this.probability;
    }

    @Override
    public int numberOfRequiredParents() {
        return 2;
    }

    @Override
    public int numberOfGeneratedChildren() {
        return 2;
    }

    @Override
    public List<BinarySolution> execute(List<BinarySolution> s) {
        Check.that(numberOfRequiredParents() == s.size(), "Point Crossover requires + " + numberOfRequiredParents() + " parents, but got " + s.size());

        if (randomNumberGenerator.nextDouble() < probability) {
            return doCrossover(s);
        } else {
            return s;
        }
    }

    protected List<BinarySolution> doCrossover(List<BinarySolution> s) {
        BinarySolution mom = s.get(0);
        BinarySolution dad = s.get(1);

        Check.that(mom.variables().size() == dad.variables().size(), "The 2 parents doesn't have the same number of variables");

        BinarySolution girl = (BinarySolution) mom.copy();
        BinarySolution boy = (BinarySolution) dad.copy();
        boolean swap = false;

        Check.that(mom.variables().get(0).getBinarySetLength() >= 2, "The number of crossovers is higher than the number of bits");
        int[] crossoverPoints = new int[2];
        for (int i = 0; i < crossoverPoints.length; i++) {
            crossoverPoints[i] = randomNumberGenerator.nextInt(0, mom.variables().get(0).getBinarySetLength() - 1);
        }

        for (int i = 0; i < mom.variables().get(0).getBinarySetLength(); i++) {
            if (swap) {
                boy.variables().get(0).set(i, mom.variables().get(0).get(i));
                girl.variables().get(0).set(i, dad.variables().get(0).get(i));
            }

            if (ArrayUtils.contains(crossoverPoints, i)) {
                swap = !swap;
            }
        }

        List<BinarySolution> result = new ArrayList<>();
        result.add(girl);
        result.add(boy);
        return result;
    }
}
