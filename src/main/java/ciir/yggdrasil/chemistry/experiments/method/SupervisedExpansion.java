package ciir.yggdrasil.chemistry.experiments.method;

import ciir.jfoley.chai.collections.util.MapFns;
import ciir.yggdrasil.chemistry.cleaned.MHNode;
import ciir.yggdrasil.chemistry.experiments.ClassificationMethod;
import ciir.yggdrasil.chemistry.experiments.ExperimentCommon;
import ciir.yggdrasil.chemistry.experiments.ExperimentResources;
import ciir.yggdrasil.galago.TemporaryGalagoIndex;
import org.lemurproject.galago.core.eval.EvalDoc;
import org.lemurproject.galago.core.eval.SimpleEvalDoc;
import org.lemurproject.galago.core.index.mem.MemoryIndex;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.retrieval.LocalRetrieval;
import org.lemurproject.galago.core.retrieval.Retrieval;
import org.lemurproject.galago.core.retrieval.query.Node;
import org.lemurproject.galago.utility.Parameters;
import org.lemurproject.galago.utility.lists.Ranked;

import java.io.IOException;
import java.util.*;

/**
 * @author jfoley
 */
public class SupervisedExpansion extends ClassificationMethod {

  private final ArrayList<MHNode> level4Nodes;
  private final ArrayList<String> allIds;
  private LocalRetrieval expansionCorpus;
  Map<String, LocalRetrieval> expandedSetupByExamSplit = new HashMap<>();
  private boolean qcm;

  public SupervisedExpansion(ExperimentResources resources) throws IOException {
    super(resources);

    Parameters argp = resources.argp;

    Map<String,List<Parameters>> questionsBySplit = new HashMap<>();
    Map<String, Set<String>> otherExams = resources.getExamSplitInfo();
    System.err.println(otherExams.size());
    assert(otherExams.size() == 23);

    for (Parameters question : resources.getAllQuestions()) {
      String exam = question.getString("exam");

      // For all other buckets, we add it if this is a training data piece for that bucket.
      for (String other : otherExams.keySet()) {
        Set<String> trainingForOther = otherExams.get(other);
        if(trainingForOther.contains(exam)) {
          MapFns.extendListInMap(questionsBySplit, other, question);
        }
      }
      /*// Question is from exam A
      // We put it in bins for exam B, C, D, etc.
      for (String split : otherExams.get(exam)) {
        assert(!Objects.equals(split, exam));
        MapFns.extendListInMap(questionsBySplit, split, question);
      }*/
    }
    System.err.println(questionsBySplit.size());
    assert(questionsBySplit.size() == 23);

    // collect list of nodes
    this.level4Nodes = new ArrayList<>();
    this.allIds = new ArrayList<>();
    for (MHNode mhNode : resources.getHierarchy().getNodes()) {
      if (mhNode.level == 4) level4Nodes.add(mhNode);
      allIds.add(mhNode.id);
    }

    int expansionDepth = (int) argp.get("node-expansion-depth", 0);
    if(expansionDepth > 0) {
      this.expansionCorpus = resources.getExpansionLM().getRetrieval();
    }

    for (Map.Entry<String, List<Parameters>> kv : questionsBySplit.entrySet()) {
      // For each exam, collect its available training data.
      String splitName = kv.getKey();
      Map<String, List<String>> textsByNode = new HashMap<>();
      for (Parameters question : kv.getValue()) {
        for (String nodeId : question.getAsList("judgments", String.class)) {
          MapFns.extendListInMap(textsByNode, nodeId, question.getString("text"));
        }
      }

      // Build a new index.
      MemoryIndex memoryIndex = TemporaryGalagoIndex.memoryIndexBuilder();

      for (MHNode mhNode : resources.getHierarchy().getNodes()) {
        memoryIndex.process(makeDocument(resources, mhNode, mhNode.descendants(), textsByNode, expansionDepth, expansionCorpus));
      }

      LocalRetrieval ret = resources.buildAndCloseLater(memoryIndex).getRetrieval();
      assert(ret != null);
      memoryIndex.close();
      expandedSetupByExamSplit.put(splitName, ret);
    }

  }


  public static Document makeDocument(ExperimentResources resources, MHNode root, List<MHNode> descendants, Map<String, List<String>> textsByNode, int expansionDepth, LocalRetrieval expansionCorpus) {
    StringBuilder sb = new StringBuilder();
    for (MHNode descendant : descendants) {
      sb.append(descendant.title).append('\n')
        .append(descendant.description).append("\n\n");
    }

    // Expand this node with its examples.
    List<String> what = textsByNode.get(root.id);
    if(what != null) {
      for (String each : what) {
        sb.append(each).append('\n');
      }
    }

    // Possibly also expand in an unsupervised manner
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

  @Override
  public String getName() {
    return "hierarchical-sdm-with-supervised-expansion";
  }

  @Override
  public List<? extends EvalDoc> predictQuestionResults(Parameters question) {
    // TODO: plan.
    // Generate documents for all levels of the hierarchy.
    // Score them all using Galago.
    // For all leaf documents, combine scores with parents.

    String exam = question.getString("exam");
    Retrieval ret = this.expandedSetupByExamSplit.get(exam);
    assert(ret != null);

    ArrayList<SimpleEvalDoc> finalResults = new ArrayList<>();
    Map<String,Double> scoreById = new HashMap<>();

    Node query = HierarchicalSDM.buildQuery(resources, question, qcm);
    List<? extends EvalDoc> results = ExperimentCommon.runWorkingSetQuery(ret, query, this.allIds);
    if(resources.argp.get("skipHierarchy", false)) {
      for (EvalDoc result : results) {
        scoreById.put(result.getName(), result.getScore());
      }
      assert (scoreById.size() == allIds.size());

      for (MHNode level4Node : level4Nodes) {
        // This should be a log score, so we're effectively running #combine()
        double combinedScore = 0.0;
        for (String id : level4Node.getNPath()) {
          Double score = scoreById.get(id);
          if (score == null) {
            System.out.println(resources.getHierarchy().lookup(id));
            throw new RuntimeException(id);
          }
          combinedScore += score;
        }
        finalResults.add(new SimpleEvalDoc(level4Node.id, -1, combinedScore));
      }

      assert (finalResults.size() == level4Nodes.size());
      Ranked.setRanksByScore(finalResults);
    } else {
      return results;
    }

    return finalResults;
  }
}
