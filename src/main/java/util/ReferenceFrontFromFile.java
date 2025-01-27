package util;

import org.apache.commons.io.FilenameUtils;
import org.uma.jmetal.solution.pointsolution.PointSolution;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.legacy.front.Front;
import org.uma.jmetal.util.legacy.front.impl.ArrayFront;
import org.uma.jmetal.util.legacy.front.util.FrontUtils;

import java.io.File;
import java.io.FileNotFoundException;

public class ReferenceFrontFromFile {
    public static void main(String[] args) throws FileNotFoundException {
        String separator = " ";

        File file = new File(args[0]);
        String name = FilenameUtils.removeExtension(file.getName());

        if (args.length > 1)
            separator = args[1];

        NonDominatedSolutionListArchive<PointSolution> nonDominatedSolutionArchive = new NonDominatedSolutionListArchive<>();
        Front front = new ArrayFront(file.getPath(), separator);
        nonDominatedSolutionArchive.addAll(FrontUtils.convertFrontToSolutionList(front));

        new SolutionListOutput(nonDominatedSolutionArchive.solutions()).printObjectivesToFile(name + ".pf", ","); //file.getParent() + '\\' +
    }
}