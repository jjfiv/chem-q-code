package ciir.yggdrasil.chemistry.experiments.method;

import ciir.jfoley.chai.random.Sample;
import ciir.yggdrasil.chemistry.cleaned.MHNode;
import ciir.yggdrasil.chemistry.experiments.ClassificationMethod;
import ciir.yggdrasil.chemistry.experiments.ExperimentResources;
import org.lemurproject.galago.core.eval.EvalDoc;
import org.lemurproject.galago.core.eval.SimpleEvalDoc;
import org.lemurproject.galago.utility.Parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author jfoley.
 */
public class RandomLeafNode extends ClassificationMethod {
  List<MHNode> leafNodes;

  public RandomLeafNode(ExperimentResources resources) {
    super(resources);
    leafNodes = new ArrayList<>();
    for (MHNode mhNode : resources.getHierarchy().getNodes()) {
      if(mhNode.level == 4) {
        leafNodes.add(mhNode);
      }
    }
  }

  @Override
  public String getName() {
    return "random-leaf-node";
  }

  @Override
  public List<? extends EvalDoc> predictQuestionResults(Parameters question) {
    int hash = question.getString("qid").hashCode();
    Random rand = new Random(hash);
    List<EvalDoc> predictions = new ArrayList<>();
    List<MHNode> sampleRandomly = Sample.byRandomWeight(leafNodes, 100, rand);
    for (int i = 0; i < sampleRandomly.size(); i++) {
      MHNode node = sampleRandomly.get(i);
      predictions.add(new SimpleEvalDoc(node.id, i+1, -Math.exp(i)));

    }
    return predictions;
  }
}
