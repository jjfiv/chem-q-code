package ciir.yggdrasil;

import ciir.yggdrasil.chemistry.BuildCategoryIndex;
import ciir.yggdrasil.chemistry.CategoryEvaluation;
import ciir.yggdrasil.chemistry.GenerateMakefile;
import ciir.yggdrasil.chemistry.SampleFromHierarchyHTML;
import ciir.yggdrasil.chemistry.experiments.CompareRunsByExam;
import ciir.yggdrasil.chemistry.experiments.FindNearDuplicates;
import ciir.yggdrasil.chemistry.experiments.PrintTable;
import ciir.yggdrasil.chemistry.experiments.RunExperiment;
import ciir.yggdrasil.stackexchange.MakePostsCommentsNeighbors;
import org.lemurproject.galago.utility.Parameters;
import org.lemurproject.galago.utility.tools.AppFunction;

import java.util.Arrays;
import java.util.Collection;

public class Main {
  public static void run(String[] args, Collection<AppFunction> fns) throws Exception {
    Parameters argp = Parameters.parseArgs(args);
    String whichFn = argp.getString("fn");
    for(AppFunction fn : fns) {
      if(whichFn.equals(fn.getName())) {
        fn.run(argp, System.out);
        return;
      }
    }
    System.err.println("Failed to find AppFunction for fn="+whichFn+" try one of:\n");
    for(AppFunction fn : fns) {
      System.err.println("--fn=" + fn.getName());
    }
  }

  public static void main(String[] args) throws Exception {
    run(args, Arrays.asList(
      new CategoryEvaluation(),
      new BuildCategoryIndex(),

      new RunExperiment(),
      new CompareRunsByExam(),
      new PrintTable(),

      new GenerateMakefile(),

      new SampleFromHierarchyHTML(),
      new FindNearDuplicates(),

      new MakePostsCommentsNeighbors()
    ));
  }
}
