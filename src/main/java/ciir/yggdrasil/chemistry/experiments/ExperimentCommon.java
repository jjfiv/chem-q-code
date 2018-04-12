package ciir.yggdrasil.chemistry.experiments;

import ciir.jfoley.chai.collections.util.ListFns;
import org.lemurproject.galago.core.eval.QueryJudgments;
import org.lemurproject.galago.core.eval.QuerySetJudgments;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.retrieval.LocalRetrieval;
import org.lemurproject.galago.core.retrieval.Results;
import org.lemurproject.galago.core.retrieval.Retrieval;
import org.lemurproject.galago.core.retrieval.ScoredDocument;
import org.lemurproject.galago.core.retrieval.query.Node;
import org.lemurproject.galago.utility.Parameters;

import java.io.IOException;
import java.util.*;

/**
 * @author jfoley.
 */
public class ExperimentCommon {
  public static Parameters DefaultParameters = Parameters.parseArray(
    "hierarchy", "hierarchies/hierarchy.fixed-jan-23.json",
    "questions", "dataset/per-test-questions.json"
  );

  public static QuerySetJudgments makeQrels(List<Parameters> questions) {
    Map<String, QueryJudgments> qrels = new HashMap<>();

    for (Parameters question : questions) {
      String qid = question.getString("qid");
      Map<String, Integer> howRelevant = new HashMap<>();

      for (String nodeName : question.getAsList("judgments", String.class)) {
        howRelevant.put(nodeName, 1);
      }

      qrels.put(qid, new QueryJudgments(qid, howRelevant));
    }

    return new QuerySetJudgments(qrels);
  }

  public static List<String> getMetrics(Parameters argp) {
    if(argp.containsKey("metrics")) {
      return argp.getAsList("metrics", String.class);
    }
    return Arrays.asList("recip_rank", "map", "ndcg", "p1", "p5", "p10", "R1", "R5", "R10");
  }

  public static List<ScoredDocument> runWorkingSetQuery(Retrieval ret, Node query, List<String> workingSet) {
    assert(ret != null);
    return runQuery(ret, query, Parameters.parseArray(
      "working", workingSet,
      "warnMissingDocuments", true,
      "requested", workingSet.size()));
  }

  public static List<ScoredDocument> runQuery(Retrieval ret, Node query) {
    assert(ret != null);
    return runQuery(ret, query, Parameters.create());
  }

  public static List<ScoredDocument> runQuery(Retrieval ret, Node query, Parameters xqp) {
    assert(ret != null);
    try {
      Parameters qp = Parameters.create();
      qp.copyFrom(xqp);
      if(!qp.containsKey("requested")) {
        qp.put("requested", 3000);
      }
      Node xq = ret.transformQuery(query, qp);
      Results results = ret.executeQuery(xq, qp);
      if(results == null) return Collections.emptyList();
      if(results.scoredDocuments == null) return Collections.emptyList();
      return results.scoredDocuments;
    } catch (Exception e) {
      System.err.println(query);
      System.err.println(xqp);
      System.err.println(e);
      System.err.println(Arrays.toString(e.getStackTrace()));
      throw new RuntimeException(e);
    }
  }

  public static List<String> pullDocuments(LocalRetrieval expansionCorpus, Node query, int expansionDepth) {
    List<String> ids = new ArrayList<>();
    for (ScoredDocument sdoc : ListFns.take(runQuery(expansionCorpus, query, Parameters.parseArray("requested", expansionDepth)), expansionDepth)) {
      ids.add(sdoc.getName());
    }

    List<String> texts = new ArrayList<>();
    try {
      Map<String, Document> documents = expansionCorpus.getDocuments(ids, Document.DocumentComponents.JustText);
      for (Document document : documents.values()) {
        texts.add(document.text);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return texts;
  }
}
