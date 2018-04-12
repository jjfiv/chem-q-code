package ciir.yggdrasil.chemistry.experiments;

import gnu.trove.map.hash.TObjectIntHashMap;
import org.lemurproject.galago.core.eval.EvalDoc;
import org.lemurproject.galago.core.eval.SimpleEvalDoc;
import org.lemurproject.galago.utility.Parameters;
import org.lemurproject.galago.utility.lists.Ranked;

import java.util.*;

/**
 * @author jfoley
 */
public class ExamsAreUniform {
  static Map<String, List<? extends EvalDoc>> attemptToLeverageUniformExamHypothesis(Parameters argp, Map<String, List<EvalDoc>> perQuestionResults) {
    Map<String, List<? extends EvalDoc>> uniformPredictions = new TreeMap<>();
    // TODO.
    if (argp.getString("uniform-method").equals("rank-freq")) {
      byRankFrequency(argp, perQuestionResults, uniformPredictions);
    } else if(argp.getString("uniform-method").equals("score-freq")) {
      byScoreFrequency(argp, perQuestionResults, uniformPredictions);
    } else if(argp.getString("uniform-method").equals("single")) {
      bySingleAssignment(perQuestionResults, uniformPredictions);
    }

    return uniformPredictions;
  }

  private static void bySingleAssignment(Map<String, List<EvalDoc>> perQuestionResults, Map<String, List<? extends EvalDoc>> uniformPredictions) {
    Set<String> alreadyUsed = new HashSet<>();
    for (String key : perQuestionResults.keySet()) {
      boolean found = false;

      for (EvalDoc evalDoc : perQuestionResults.get(key)) {
        if (alreadyUsed.contains(evalDoc.getName())) {
          continue;
        }
        alreadyUsed.add(evalDoc.getName());
        uniformPredictions.put(key, Collections.singletonList(evalDoc));
        found = true;
        break;
      }

      if (!found) {
        uniformPredictions.put(key, Collections.<EvalDoc>emptyList());
      }
    }
  }

  private static void byScoreFrequency(Parameters argp, Map<String, List<EvalDoc>> perQuestionResults, Map<String, List<? extends EvalDoc>> uniformPredictions) {
    // freq scores across test.
    TObjectIntHashMap<String> hist = new TObjectIntHashMap<>();
    int uniformDepth = (int) argp.getLong("uniform-depth");

    // accumulate freq scores.
    for (List<EvalDoc> evalDocs : perQuestionResults.values()) {
      for (int i = 0; i < Math.min(uniformDepth, evalDocs.size()); i++) {
        hist.adjustOrPutValue(evalDocs.get(i).getName(), 1, 1);
      }
    }

    // rerank by 1/r + 1/f
    for (String key : perQuestionResults.keySet()) {
      List<EvalDoc> evalDocs = perQuestionResults.get(key);

      List<SimpleEvalDoc> newDocuments = new ArrayList<>();
      for (int i = 0; i < Math.min(uniformDepth, evalDocs.size()); i++) {
        String name = evalDocs.get(i).getName();
        double irank = 1.0 / ((double) evalDocs.get(i).getRank());
        double ifreq = 1.0 / ((double) hist.get(name));

        newDocuments.add(new SimpleEvalDoc(name, -1, 0.8 * evalDocs.get(i).getScore() +  0.2 * ifreq));
      }

      Ranked.setRanksByScore(newDocuments);
      uniformPredictions.put(key, newDocuments);
    }
  }

  private static void byRankFrequency(Parameters argp, Map<String, List<EvalDoc>> perQuestionResults, Map<String, List<? extends EvalDoc>> uniformPredictions) {
    // freq scores across test.
    TObjectIntHashMap<String> hist = new TObjectIntHashMap<>();
    int uniformDepth = (int) argp.getLong("uniform-depth");

    // accumulate freq scores.
    for (List<EvalDoc> evalDocs : perQuestionResults.values()) {
      for (int i = 0; i < Math.min(uniformDepth, evalDocs.size()); i++) {
        hist.adjustOrPutValue(evalDocs.get(i).getName(), 1, 1);
      }
    }

    // rerank by 1/r + 1/f
    for (String key : perQuestionResults.keySet()) {
      List<EvalDoc> evalDocs = perQuestionResults.get(key);

      List<SimpleEvalDoc> newDocuments = new ArrayList<>();
      for (int i = 0; i < Math.min(uniformDepth, evalDocs.size()); i++) {
        String name = evalDocs.get(i).getName();
        double irank = 1.0 / evalDocs.get(i).getRank();
        double ifreq = 1.0 / hist.get(name);

        newDocuments.add(new SimpleEvalDoc(name, -1, 0.8 * irank + 0.2 * ifreq));
      }

      Ranked.setRanksByScore(newDocuments);
      uniformPredictions.put(key, newDocuments);
    }
  }
}
