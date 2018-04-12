package ciir.yggdrasil.chemistry.experiments;

import ciir.jfoley.chai.collections.util.IterableFns;
import ciir.jfoley.chai.collections.util.MapFns;
import ciir.jfoley.chai.string.StrUtil;
import org.lemurproject.galago.utility.Parameters;
import org.lemurproject.galago.utility.tools.AppFunction;

import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jfoley.
 */
public class PrintTable extends AppFunction {
  @Override
  public String getName() {
    return "print-table";
  }

  @Override
  public String getHelpString() {
    return makeHelpStr(
      "runs", "list of run .json files",
      "metrics", "ndcg, recip_rank, R1"
    );
  }

  public String withoutPaths(String input) {
    File fp = new File(input);
    return fp.getName();
  }

  public static Map<String, List<Double>> getByExamScores(Parameters values, List<String> metrics) {
    Parameters byExam = values.getMap("byExam");
    // Accumulate across exam.
    Map<String, List<Double>> data = new HashMap<>();
    for (String exam : IterableFns.sorted(byExam.keySet())) {
      for (String metric : metrics) {
        double value = byExam.getMap(exam).getDouble(metric);
        MapFns.extendListInMap(data, metric, value);
      }
    }
    return data;
  }

  public static double mean(List<Double> xs) {
    double sum = 0.0;
    for (double x : xs) {
      sum += x;
    }
    return sum / xs.size();
  }

  @Override
  public void run(Parameters p, PrintStream output) throws Exception {
    List<String> metrics = Arrays.asList("recip_rank", "ndcg", "R1");
    if(p.isList("metrics")) {
      metrics = p.getAsList("metrics", String.class);
    }

    // LaTeX header row.
    output.printf("%-20s ", "Run Id");
    for (String metric : metrics) {
      output.printf("& %s ", metric);
    }
    output.println(" \\\\");

    for (String run : p.getAsList("runs", String.class)) {
      String runId = StrUtil.removeBack(withoutPaths(run), ".json");
      Parameters values = Parameters.parseFile(run);

      Map<String, List<Double>> data = getByExamScores(values, metrics);


      // Print one row with a summary stat.
      output.printf("%-20s ", runId);
      for (String metric : metrics) {
        output.printf("& %1.3f ", mean(data.get(metric)));
      }
      output.println("\\\\");

    }
  }
}
