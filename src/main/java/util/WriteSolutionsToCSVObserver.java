package util;

import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observable.Observable;
import org.uma.jmetal.util.observer.Observer;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This observer stores a solution list in files. Concretely, the variables and objectives are written in files called
 * VAR.x.tsv and VAR.x.tsv, respectively (x is an iteration counter). The frequency of the writes are set by a
 * parameter.
 *
 * @author Antonio J. Nebro
 */
public class WriteSolutionsToCSVObserver implements Observer<Map<String, Object>> {

    private Integer frequency;
    private int counter;
    private String outputDirectory = "";

    /**
     * Constructor
     */

    public WriteSolutionsToCSVObserver(Integer frequency, String outputDirectory) {
        this.frequency = frequency;
        this.counter = 0;
        this.outputDirectory = outputDirectory;

        File file = new File(outputDirectory);

        if (file.exists()) {
            if (file.isFile()) {
                throw new RuntimeException(outputDirectory + " exists and it is a file");
            } else if (file.isDirectory()) {
                for (File child : Objects.requireNonNull(file.listFiles()))
                    deleteRecursively(child);
            }
        } else {
            if (!file.mkdir()) {
                throw new RuntimeException("Unable to create the directory");
            }
        }
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                deleteRecursively(child);
            }
        }
        if (!file.delete()) {
            throw new RuntimeException("Failed to delete " + file.getAbsolutePath());
        }
    }

    public WriteSolutionsToCSVObserver() {
        this(1, "outputDirectory");
    }

    /**
     * This method gets the population
     *
     * @param data Map of pairs (key, value)
     */
    @Override
    public void update(Observable<Map<String, Object>> observable, Map<String, Object> data) {
        List<?> population = (List<?>) data.get("POPULATION");

        if (!population.isEmpty()) {
            if (counter % frequency == 0) {
                JMetalLogger.logger.info("Trace saved");
                new SolutionListOutput((List<? extends Solution<?>>) population)
                        .setVarFileOutputContext(new DefaultFileOutputContext(outputDirectory + "/VAR." + counter + ".csv", ","))
                        .setFunFileOutputContext(new DefaultFileOutputContext(outputDirectory + "/FUN." + counter + ".csv", ","))
                        .print();
            }
        } else {
            if (counter != 0)
                JMetalLogger.logger.warning(getClass().getName() + ": The POPULATION is empty");
        }

        counter++;
    }

    public String getName() {
        return "Print objectives observer";
    }

    @Override
    public String toString() {
        return getName();
    }
}
