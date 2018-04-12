package ciir.yggdrasil.chemistry.experiments;

import org.lemurproject.galago.core.eval.EvalDoc;
import org.lemurproject.galago.core.retrieval.query.Node;
import org.lemurproject.galago.utility.Parameters;

import java.util.List;

/**
* @author jfoley.
*/
public abstract class ClassificationMethod {
  protected final ExperimentResources resources;

  public ClassificationMethod(ExperimentResources resources) {
    this.resources = resources;
  }

  public static Node buildQueryFromQuestion(ExperimentResources resources, Parameters question) {
    List<String> terms = resources.tokenizer.tokenize(question.getString("text")).terms;
    Node query = new Node(resources.argp.get("retrievalModel", "sdm"));

    for (String term : terms) {
      if(resources.getStopwords().contains(term)) continue;
      query.addChild(Node.Text(term));
    }

    return query;
  }

  public Node buildQueryFromQuestion(Parameters question) {
    return buildQueryFromQuestion(resources, question);
  }

  public Parameters argp() { return resources.argp; }

  abstract public String getName();
  abstract public List<? extends EvalDoc> predictQuestionResults(Parameters question);
}
