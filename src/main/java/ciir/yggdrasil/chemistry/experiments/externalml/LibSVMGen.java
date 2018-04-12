package ciir.yggdrasil.chemistry.experiments.externalml;

import ciir.jfoley.chai.collections.Pair;
import ciir.jfoley.chai.collections.util.IterableFns;
import ciir.jfoley.chai.collections.util.ListFns;
import ciir.jfoley.chai.collections.util.MapFns;
import ciir.jfoley.chai.io.IO;
import ciir.yggdrasil.chemistry.cleaned.MHNode;
import ciir.yggdrasil.chemistry.experiments.ExperimentCommon;
import ciir.yggdrasil.chemistry.experiments.ExperimentResources;
import org.lemurproject.galago.core.parse.stem.KrovetzStemmer;
import org.lemurproject.galago.core.parse.stem.Stemmer;
import org.lemurproject.galago.utility.Parameters;
import org.lemurproject.galago.utility.tools.AppFunction;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.*;

/**
 * @author jfoley.
 */
public class LibSVMGen extends AppFunction {
  @Override
  public String getName() {
    return "libsvm-gen";
  }

  @Override
  public String getHelpString() {
    return makeHelpStr("defaults", "etc...");
  }

  public static int positiveHash(Object obj) {
    return Math.abs(obj.hashCode());
  }

  public static void putOrIncrement(Map<Integer, Double> map, int fid, double val) {
    double newVal = val;
    Double original = map.get(fid);
    if (original != null) {
      newVal += original;
    }
    map.put(fid, newVal);
  }

  public static Map<Integer, Double> generateFeatures(List<String> terms) {
    Map<Integer, Double> output = new HashMap<>();

    // unigram
    for (String term : terms) {
      int fid = positiveHash(term) % 10000;
      putOrIncrement(output, fid, 1.0 / terms.size());
    }

    // bigram
    for (List<String> lr : ListFns.sliding(terms, 2)) {
      String left = lr.get(0);
      String right = lr.get(1);
      int fid = ((positiveHash(left) ^ positiveHash(right)) % 10000) + 10000;
      putOrIncrement(output, fid, 1.0 / terms.size());
    }

    // unordered:8
    for (List<String> windows : ListFns.sliding(terms, 8)) {
      for (Pair<String, String> kv : ListFns.pairs(windows)) {
        int fid = (positiveHash(kv) % 10000) + 20000;
        putOrIncrement(output, fid, 1.0 / terms.size());
      }
    }

    return output;
  }

  @Override
  public void run(Parameters p, PrintStream stdout) throws Exception {
    Parameters argp = p.clone();
    argp.copyFrom(ExperimentCommon.DefaultParameters);

    // Generate 1 class per nodeId
    // Generate 1 line per question, with classes marked as judgments
    // X,Y fid:fval ...

    List<String> allIds = new ArrayList<>();

    Map<String, List<String>> ptsBySplit = new HashMap<>();

    try (ExperimentResources resources = new ExperimentResources(argp)) {

      Map<String, Integer> nodeToClass = new HashMap<>();

      int classNum = 0;
      for (MHNode mhNode : resources.getHierarchy().getNodes()) {
        nodeToClass.put(mhNode.id, classNum++);
      }

      // Generate features for all questions.
      List<Parameters> allQuestions = resources.getAllQuestions();
      for (Parameters question : allQuestions) {
        List<String> qterms = resources.tokenizer.tokenize(question.getString("text")).terms;

        List<String> sterms = new ArrayList<>();
        Stemmer stemmer = KrovetzStemmer.create(argp);
        for (String qterm : qterms) {
          sterms.add(stemmer.stem(qterm));
        }

        List<Integer> classes = new ArrayList<>();
        for (String nodeId : question.getAsList("judgments", String.class)) {
          classes.add(nodeToClass.get(nodeId));
        }

        for (int label : classes) {
          // features.
          final StringBuilder svmf = new StringBuilder();
          // class
          svmf.append(label).append(' ');

          List<Map.Entry<Integer,Double>> features = IterableFns.sorted(generateFeatures(sterms).entrySet(),
              new Comparator<Map.Entry<Integer, Double>>() {
                @Override
                public int compare(Map.Entry<Integer, Double> lhs, Map.Entry<Integer, Double> rhs) {
                  return Integer.compare(lhs.getKey(), rhs.getKey());
                }
              });


          for (Map.Entry<Integer, Double> kv : features) {
            svmf.append(kv.getKey()).append(':').append(kv.getValue()).append(' ');
          }

          String examName = question.getString("exam");
          int id = examName.hashCode() % 20;
          String split;
          if (id < 10) {
            split = "libsvm-train";
          } else if (id < 15) {
            split = "libsvm-validate";
          } else {
            split = "libsvm-eval";
          }
          MapFns.extendListInMap(ptsBySplit, split, svmf.toString());
        }
      }
    }

    // Write by split.
    for (String split : ptsBySplit.keySet()) {
      System.err.println("Writing "+split+".svm");
      try (PrintWriter splitSVM = IO.openPrintWriter(split + ".svm")) {
        for (String data : ptsBySplit.get(split)) {
          splitSVM.println(data);
        }
      }
    }

    System.err.println("Finished!");
  }

  public static void main(String[] args) throws Exception {
    Parameters argp = Parameters.parseArgs(args);
    AppFunction fn = new LibSVMGen();
    fn.run(argp, System.out);
  }
}
