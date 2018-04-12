package ciir.yggdrasil.chemistry;

import ciir.jfoley.chai.Checked;
import ciir.jfoley.chai.collections.util.IterableFns;
import ciir.jfoley.chai.random.Sample;
import ciir.yggdrasil.util.TSVLoader;
import org.lemurproject.galago.core.eval.*;
import org.lemurproject.galago.core.eval.aggregate.QuerySetEvaluator;
import org.lemurproject.galago.core.eval.aggregate.QuerySetEvaluatorFactory;
import org.lemurproject.galago.core.eval.metric.QueryEvaluator;
import org.lemurproject.galago.core.eval.metric.QueryEvaluatorFactory;
import org.lemurproject.galago.core.retrieval.LocalRetrieval;
import org.lemurproject.galago.core.retrieval.Results;
import org.lemurproject.galago.core.retrieval.query.Node;
import org.lemurproject.galago.utility.Parameters;
import org.lemurproject.galago.utility.tools.AppFunction;

import java.io.PrintStream;
import java.util.*;

/**
 * @author jfoley.
 */
public class CategoryEvaluation extends AppFunction {

  public static void main(String[] args) throws Exception {
    AppFunction me = new CategoryEvaluation();
    Parameters argp = Parameters.parseArgs(args);
    me.run(argp, System.out);
  }

  private static QuerySetJudgments loadQrels(List<Parameters> questions, List<Parameters> rows) {
    Map<String,QueryJudgments> qrels = new HashMap<>();
    for (Parameters question : questions) {
      if (!question.containsKey("judgment")) continue;
      Map<String, Integer> queryJudgments = new HashMap<>();
      String qid = "Q"+question.getLong("id");

      Set<Integer> judgments = new HashSet<>(question.getAsList("judgment", Integer.class));

      for (Parameters row : rows) {
        int id4 = (int) row.getLong("FrameworkCategoryID4");
        int id3 = (int) row.getLong("FrameworkCategoryID3");
        int id2 = (int) row.getLong("FrameworkCategoryID2");
        int id1 = (int) row.getLong("FrameworkCategoryID1");
        for(int x : new int[]{id4}) {
          if (judgments.contains(x)) {
            queryJudgments.put("C" + x, 1);
          }
        }
      }

      // Save into qrels structure.
      qrels.put(qid, new QueryJudgments(qid, queryJudgments));
    }
    return new QuerySetJudgments(qrels);
  }

  @Override
  public String getName() {
    return "category-evaluation";
  }

  @Override
  public String getHelpString() {
    return makeHelpStr(
        "output", "eval.json - metric on per-query level as json",
        "metrics", "[recip_rank, map, ndcg...]",
        "category-index", "default=indices/flat-categories.galago"
        );
  }

  @Override
  public void run(Parameters argp, PrintStream stdout) throws Exception {
    String outputData = argp.getString("output");

    List<String> metrics = Arrays.asList("recip_rank", "map", "ndcg", "p5", "p10", "R1", "R5", "R10");
    if(argp.containsKey("metrics")) {
      metrics = argp.getAsList("metrics", String.class);
    }

    List<Parameters> questions = OWLGeneratedQuestionLoader.load();
    //LocalRetrieval ret = new LocalRetrieval(CategoriesLoader.loadCategoriesInMemory("chemistry/flat-framework.tsv"));
    List<Parameters> rows = TSVLoader.withHeader("chemistry/flat-framework.tsv");
    LocalRetrieval ret = new LocalRetrieval(argp.get("category-index", "indices/flat-categories.galago"));

    QuerySetJudgments qrel = loadQrels(questions, rows);
    Map<String,List<EvalDoc>> results = new HashMap<>();

    List<Parameters> evaluationData = new ArrayList<>();

    Parameters randomQ = IterableFns.first(Sample.byRandomWeight(questions, 1, new Random()));
    System.out.println(randomQ.toPrettyString());

    for (Parameters question : questions) {
      if(!question.containsKey("judgment")) continue;

      // Skip queries with no judgments.
      String qid = "Q"+question.getLong("id");
      if(qrel.get(qid).isEmpty()) continue;

      //System.out.println(question.keySet());
      Node q = new Node(argp.get("model", "combine"));
      List<String> terms = question.getAsList("terms", String.class);
      for (String term : terms) {
        q.addChild(Node.Text(term));
      }
      Parameters qp = Parameters.create();
      Node xq = ret.transformQuery(q, qp);
      Results gResults = ret.executeQuery(xq, qp);

      Parameters qeval = Parameters.create();
      qeval.put("qid", qid);
      for (String metric : metrics) {
        QueryEvaluator qse = QueryEvaluatorFactory.create(metric, argp);
        qeval.put(metric,
            qse.evaluate(new QueryResults(gResults.scoredDocuments), qrel.get(qid)));
      }
      evaluationData.add(qeval);
      results.put(qid, Checked.<List<EvalDoc>>cast(gResults.scoredDocuments));
    }

    // Evaluate
    Parameters scores = Parameters.create();
    QuerySetResults qsr = new QuerySetResults(results);
    for (String metric : metrics) {
      QuerySetEvaluator qse = QuerySetEvaluatorFactory.create(metric, argp);
      scores.put(metric, qse.evaluate(qsr, qrel));
    }
    System.out.println(scores);

    // Save data as json.
    Parameters.parseArray("data", evaluationData).write(outputData);
  }
}
