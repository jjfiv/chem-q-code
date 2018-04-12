package ciir.yggdrasil.chemistry.experiments;

import ciir.yggdrasil.chemistry.experiments.method.*;

/**
 * @author jfoley.
 */
public class ClassificationMethodFactory {
  public static ClassificationMethod create(ExperimentResources resources) {
    String method = resources.argp.getString("method");
    try {
      switch (method) {
        case "unsupervised":
          return new UnsupervisedBaseline(resources);
        case "question-knn":
          return new QuestionKNN(resources);
        case "hierarchical-sdm":
          return new HierarchicalSDM(resources);
        case "supervised-expansion":
          return new SupervisedExpansion(resources);
        case "random":
          return new RandomLeafNode(resources);
        case "oracle":
          return new OraclePrediction(resources);
        case "liblinear":
          return new LibLinearClassifier(resources);
        default:
          throw new RuntimeException("Not sure how to run method=" + method);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
