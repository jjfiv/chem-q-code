package ciir.yggdrasil.chemistry.experiments.method;

import ciir.yggdrasil.chemistry.cleaned.MHNode;
import ciir.yggdrasil.chemistry.experiments.ClassificationMethod;
import ciir.yggdrasil.chemistry.experiments.ExperimentCommon;
import ciir.yggdrasil.chemistry.experiments.ExperimentResources;
import ciir.yggdrasil.galago.TemporaryGalagoIndex;
import org.lemurproject.galago.core.eval.EvalDoc;
import org.lemurproject.galago.core.index.mem.MemoryIndex;
import org.lemurproject.galago.core.retrieval.LocalRetrieval;
import org.lemurproject.galago.core.retrieval.query.Node;
import org.lemurproject.galago.utility.Parameters;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
* @author jfoley.
*/
public class UnsupervisedBaseline extends ClassificationMethod {
  private final LocalRetrieval ret;
  private LocalRetrieval expansionCorpus;
  private boolean qcm;

  public UnsupervisedBaseline(ExperimentResources resources) throws IOException {
    super(resources);

    Parameters argp = resources.argp;
    qcm = argp.get("question-context-model", false);

    int expansionDepth = (int) argp.get("node-expansion-depth", 0);
    if(expansionDepth > 0) {
      this.expansionCorpus = resources.getExpansionLM().getRetrieval();
    }

    MemoryIndex index = TemporaryGalagoIndex.memoryIndexBuilder();
    for (MHNode mhNode : resources.getHierarchy().getNodes()) {
      index.process(
        HierarchicalSDM.makeDocument(resources, mhNode, Collections.singletonList(mhNode), expansionDepth, expansionCorpus));
    }
    this.ret = resources.buildAndCloseLater(index).getRetrieval();

  }

  @Override
  public String getName() {
    return "unsupervised";
  }

  @Override
  public List<? extends EvalDoc> predictQuestionResults(Parameters question) {
    Node query = HierarchicalSDM.buildQuery(resources, question, qcm);
    return ExperimentCommon.runQuery(ret, query);
  }
}
