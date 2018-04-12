package ciir.yggdrasil.chemistry.experiments.method;

import ciir.jfoley.chai.collections.util.MapFns;
import ciir.yggdrasil.chemistry.experiments.ClassificationMethod;
import ciir.yggdrasil.chemistry.experiments.ExperimentCommon;
import ciir.yggdrasil.chemistry.experiments.ExperimentResources;
import gnu.trove.list.array.TDoubleArrayList;
import org.lemurproject.galago.core.eval.EvalDoc;
import org.lemurproject.galago.core.eval.SimpleEvalDoc;
import org.lemurproject.galago.core.retrieval.LocalRetrieval;
import org.lemurproject.galago.core.retrieval.ScoredDocument;
import org.lemurproject.galago.core.retrieval.query.Node;
import org.lemurproject.galago.utility.MathUtils;
import org.lemurproject.galago.utility.Parameters;
import org.lemurproject.galago.utility.lists.Ranked;

import java.io.IOException;
import java.util.*;

/**
 * @author jfoley.
 */
public class QuestionKNN extends ClassificationMethod {
  private final LocalRetrieval ret;
  private final String aggregateMethod;
  private final int depth;
  private final boolean qcm;
  private final Map<String,Set<String>> trainingExams;

  public QuestionKNN(ExperimentResources resources) {
    super(resources);
    this.aggregateMethod = resources.argp.getString("knnVotingMethod");
    this.depth = (int) resources.argp.getLong("knnDepth");

    qcm = resources.argp.get("question-context-model", false);
    trainingExams = resources.getExamSplitInfo();

    if(resources.argp.get("knnIncludeHierarchy", false)) {
      this.ret = resources.getQuestionsAndNodesLM().getRetrieval();
    } else {
      this.ret = resources.getQuestionLM().getRetrieval();
    }
  }

  @Override
  public String getName() {
    return "question-knn:"+aggregateMethod;
  }

  public static class NodeVote {
    /** rank of the question itself */
    int rank;
    /** score of the question itself */
    double questionScore;

    public NodeVote(int rank, double score) {
      this.rank = rank;
      this.questionScore = score;
    }
  }

  @Override
  public List<? extends EvalDoc> predictQuestionResults(Parameters question) {
    List<SimpleEvalDoc> predictions = new ArrayList<>();
    Map<String, List<NodeVote>> nodeVotes = new HashMap<>();

    String currentExam = question.getString("exam");

    Node query = HierarchicalSDM.buildQuery(resources, question, qcm);
    for (ScoredDocument sdoc : ExperimentCommon.runQuery(ret, query, Parameters.parseArray("requested", depth))) {
      if(sdoc.rank > depth) continue;
      try {
        Parameters info = Parameters.parseString(sdoc.documentName);

        // Hack for leave-one-out cross fold validation.
        // Skip results that are from the exam we're currently scoring.
        String exampleExam = info.getString("exam");
        Set<String> trainExams = trainingExams.get(currentExam);
        if(!trainExams.contains(exampleExam)) {
          continue;
        }

        for (String node : info.getAsList("judgments", String.class)) {
          MapFns.extendListInMap(nodeVotes, node, new NodeVote(sdoc.rank, sdoc.score));
        }
      } catch (IOException e) {
        continue;
      }
    }

    for (Map.Entry<String, List<NodeVote>> kv : nodeVotes.entrySet()) {
      String nodeName = kv.getKey();
      double newScore = aggregateNodeVotes(aggregateMethod, kv.getValue());
      predictions.add(new SimpleEvalDoc(nodeName, -1, newScore));
    }

    Ranked.setRanksByScore(predictions);

    return predictions;
  }

  public static double aggregateNodeVotes(String aggregateMethod, List<NodeVote> votes) {
    double score = 0.0;
    switch (aggregateMethod) {
      /** "Best" unsupervised */
      case "ranksum":
        for (NodeVote vote : votes) {
          score += (1.0 / vote.rank);
        }
        return score;
      case "scorewsum": {
        TDoubleArrayList scores = new TDoubleArrayList();
        for (NodeVote vote : votes) {
          scores.add(vote.questionScore);
        }
        return MathUtils.logSumExp(scores.toArray());
      }
      case "scoresum":
        for (NodeVote vote : votes) {
          score += vote.questionScore;
        }
        return score;
      /**
       * Count up the times it shows up in top whatever
       * This ends up being true KNN.
       */
      case "count":
        return votes.size();
      default:
        throw new RuntimeException("Not implemented: aggregateNodeVotes: "+aggregateMethod);
    }
  }
}
