package ciir.yggdrasil.chemistry.experiments.method;

import ciir.yggdrasil.chemistry.cleaned.MHNode;
import ciir.yggdrasil.chemistry.experiments.ClassificationMethod;
import ciir.yggdrasil.chemistry.experiments.ExperimentCommon;
import ciir.yggdrasil.chemistry.experiments.ExperimentResources;
import ciir.yggdrasil.chemistry.experiments.QuestionContextModelParser;
import ciir.yggdrasil.galago.TemporaryGalagoIndex;
import org.lemurproject.galago.core.eval.EvalDoc;
import org.lemurproject.galago.core.eval.SimpleEvalDoc;
import org.lemurproject.galago.core.index.mem.MemoryIndex;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.retrieval.LocalRetrieval;
import org.lemurproject.galago.core.retrieval.query.Node;
import org.lemurproject.galago.utility.Parameters;
import org.lemurproject.galago.utility.lists.Ranked;

import java.io.IOException;
import java.util.*;

/**
 * @author jfoley
 */
public class HierarchicalSDM extends ClassificationMethod {

  private final TemporaryGalagoIndex index;
  private final ArrayList<MHNode> level4Nodes;
  private final ArrayList<String> allIds;
  private LocalRetrieval expansionCorpus;
  private boolean qcm;

  public HierarchicalSDM(ExperimentResources resources) {
    super(resources);

    Parameters argp = resources.argp;

    qcm = argp.get("question-context-model", false);

    try {
      this.level4Nodes = new ArrayList<>();
      this.allIds = new ArrayList<>();

      int expansionDepth = (int) argp.get("node-expansion-depth", 0);
      if(expansionDepth > 0) {
        this.expansionCorpus = resources.getExpansionLM().getRetrieval();
      }

      boolean expandDescendants = argp.get("expand-descendants", true);

      // Build a new index.
      MemoryIndex memoryIndex = TemporaryGalagoIndex.memoryIndexBuilder();
      for (MHNode mhNode : resources.getHierarchy().getNodes()) {
        if(mhNode.level == 4) level4Nodes.add(mhNode);
        allIds.add(mhNode.id);
        memoryIndex.process(makeDocument(resources, mhNode, expandDescendants ? mhNode.descendants() : Collections.singletonList(mhNode), expansionDepth, expansionCorpus));
      }
      this.index = resources.buildAndCloseLater(memoryIndex);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static Document makeDocument(ExperimentResources resources, MHNode root, List<MHNode> descendants, int expansionDepth, LocalRetrieval expansionCorpus) {
    StringBuilder sb = new StringBuilder();
    for (MHNode descendant : descendants) {
      sb.append(descendant.title).append('\n')
        .append(descendant.description).append("\n\n");
    }

    if(expansionDepth > 0) {
      Node query = resources.buildQuery(root.title + " " + root.description);
      for (String xdoc : ExperimentCommon.pullDocuments(expansionCorpus, query, expansionDepth)) {
        sb.append(xdoc).append('\n');
      }
    }

    Document doc = new Document();
    doc.name = root.id;
    doc.text = sb.toString();
    resources.tokenizer.tokenize(doc);
    return doc;
  }

  public static Node buildQuery(ExperimentResources resources, Parameters question, boolean qcm) {
    Node query = buildQueryFromQuestion(resources, question);
    if(qcm) {
      List<String> terms = resources.tokenizer.tokenize(QuestionContextModelParser.extractQuestionPieces(question.getString("text")).get("question-statement")).terms;
      Node questionStatementQ = new Node(resources.argp.get("retrievalModel", "sdm"));

      for (String term : terms) {
        if(resources.getStopwords().contains(term)) continue;
        questionStatementQ.addChild(Node.Text(term));
      }

      Node original = query;
      query = new Node("combine");
      query.addChild(original);
      query.addChild(questionStatementQ);
    }
    return query;
  }

  @Override
  public String getName() {
    return "hierarchical-sdm";
  }

  @Override
  public List<? extends EvalDoc> predictQuestionResults(Parameters question) {
    // TODO: plan.
    // Generate documents for all levels of the hierarchy.
    // Score them all using Galago.
    // For all leaf documents, combine scores with parents.

    Node query = buildQuery(resources, question, qcm);

    ArrayList<SimpleEvalDoc> finalResults = new ArrayList<>();
    Map<String,Double> scoreById = new HashMap<>();
    List<? extends EvalDoc> results = ExperimentCommon.runWorkingSetQuery(index.getRetrieval(), query, this.allIds);
    for (EvalDoc result : results) {
      scoreById.put(result.getName(), result.getScore());
    }
    assert(scoreById.size() == allIds.size());

    for (MHNode level4Node : level4Nodes) {
      // This should be a log score, so we're effectively running #combine()
      double combinedScore = 0.0;
      for(String id : level4Node.getNPath()) {
        Double score = scoreById.get(id);
        if(score == null) {
          System.out.println(resources.getHierarchy().lookup(id));
          throw new RuntimeException(id);
        }
        combinedScore += score;
      }
      finalResults.add(new SimpleEvalDoc(level4Node.id, -1, combinedScore));
    }

    assert(finalResults.size() == level4Nodes.size());
    Ranked.setRanksByScore(finalResults);

    return finalResults;
  }
}
