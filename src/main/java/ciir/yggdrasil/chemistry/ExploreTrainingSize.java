package ciir.yggdrasil.chemistry;

import ciir.jfoley.chai.collections.Pair;
import ciir.jfoley.chai.collections.util.IterableFns;
import ciir.jfoley.chai.collections.util.MapFns;
import ciir.yggdrasil.chemistry.experiments.PrintTable;
import org.lemurproject.galago.tupleflow.FileUtility;
import org.lemurproject.galago.utility.Parameters;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author jfoley
 */
public class ExploreTrainingSize {

  static String getMethodName(Parameters args, String filename) {
    String method = args.getString("method");
    switch (method) {
      case "question-knn":
        if (args.getBoolean("knnIncludeHierarchy")) {
          return String.format("KNN (%d) Q+N", args.getLong("knnDepth"));
        } else {
          return String.format("KNN (%d) Q", args.getLong("knnDepth"));
        }
      case "supervised-expansion":
        return method;
      default:
        System.out.println(filename);
        throw new RuntimeException(args.toString());
    }
  }

  public static void main(String[] args) throws IOException {
    List<String> metrics = Arrays.asList("recip_rank", "ndcg", "R1");
    List<Parameters> candidates = new ArrayList<>();
    for (File run : FileUtility.safeListFiles(new File("runs"))) {
      if(run.getName().contains("only")) {
        Parameters pf = Parameters.parseFile(run);
        pf.put("filename", run.getName());
        candidates.add(pf);
      }
    }

    Map<String, List<Parameters>> resultsByMethod = new HashMap<>();
    for (Parameters candidate : candidates) {
      Parameters ca = candidate.getMap("argp");
      MapFns.extendListInMap(resultsByMethod, getMethodName(ca, candidate.getString("filename")), candidate);
    }

    Map<Pair<String,Integer>, List<Double>> groupByMethodAndDepth = new HashMap<>();

    for (Map.Entry<String, List<Parameters>> kv : resultsByMethod.entrySet()) {
      for (Parameters parameters : kv.getValue()) {
        int depth = (int) parameters.getMap("argp").getLong("downsample");
        Map<String, List<Double>> byExamScores = PrintTable.getByExamScores(parameters, metrics);
        //System.out.printf("%s\t%d\t%1.3f\n", kv.getKey(), depth, PrintTable.mean(byExamScores.get("ndcg")));
        MapFns.extendListInMap(groupByMethodAndDepth, Pair.of(kv.getKey(), depth), PrintTable.mean(byExamScores.get("ndcg")));
      }
    }

    Map<Integer, List<Pair<String,Double>>> invertedForSheets = new HashMap<>();

    for (Map.Entry<Pair<String, Integer>, List<Double>> kv : groupByMethodAndDepth.entrySet()) {
      String method = kv.getKey().left;
      int depth = kv.getKey().right;
      double macroMean = PrintTable.mean(kv.getValue());
      //System.out.printf("%s\t%d\t%1.3f\n", method, depth, macroMean);

      MapFns.extendListInMap(invertedForSheets, depth, Pair.of(method, macroMean));
    }

    System.out.print("Num Training Exams");
    Comparator<Pair<String, Double>> pairKeyAlpha = new Comparator<Pair<String, Double>>() {
      @Override
      public int compare(Pair<String, Double> lhs, Pair<String, Double> rhs) {
        return lhs.left.compareTo(rhs.left);
      }
    };
    for (Pair<String, Double> kv : IterableFns.sorted(MapFns.firstValue(invertedForSheets), pairKeyAlpha)) {
      System.out.printf("\t%s", kv.getKey());
    }
    System.out.println();

    for (Map.Entry<Integer, List<Pair<String, Double>>> kv : invertedForSheets.entrySet()) {
      List<Pair<String,Double>> runs = IterableFns.sorted(kv.getValue(), pairKeyAlpha);
      System.out.printf("%d", kv.getKey());
      for (Pair<String, Double> run : runs) {
        System.out.printf("\t%1.3f", run.getValue());
      }
      System.out.println();
    }

  }
}
