package ciir.yggdrasil.chemistry.experiments.method;

import ciir.yggdrasil.chemistry.cleaned.MHNode;
import ciir.yggdrasil.chemistry.experiments.ClassificationMethod;
import ciir.yggdrasil.chemistry.experiments.ExperimentResources;
import org.lemurproject.galago.core.eval.EvalDoc;
import org.lemurproject.galago.core.eval.SimpleEvalDoc;
import org.lemurproject.galago.utility.Parameters;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jfoley.
 */
public class OraclePrediction extends ClassificationMethod {
  public OraclePrediction(ExperimentResources resources) {
    super(resources);
  }

  @Override
  public String getName() {
    return "oracle";
  }

  @Override
  public List<? extends EvalDoc> predictQuestionResults(Parameters question) {
    List<EvalDoc> predictions = new ArrayList<>();
    List<String> oracleIds = question.getAsList("judgments", String.class);
    int rank = 0;
    for (String id : oracleIds) {
      ++rank;
      predictions.add(new SimpleEvalDoc(id, rank, -Math.exp(rank)));
    }
    for (MHNode node : resources.hierarchy.getNodes()) {
      if(oracleIds.contains(node.id)) continue;
      ++rank;
      predictions.add(new SimpleEvalDoc(node.id, rank, -Math.exp(rank)));
    }
    return predictions;
  }
}
