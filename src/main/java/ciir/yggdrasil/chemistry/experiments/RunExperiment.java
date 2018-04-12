package ciir.yggdrasil.chemistry.experiments;

import ciir.jfoley.chai.Checked;
import ciir.jfoley.chai.io.IO;
import ciir.yggdrasil.chemistry.cleaned.MergedHierarchy;
import org.lemurproject.galago.core.eval.*;
import org.lemurproject.galago.core.eval.aggregate.QuerySetEvaluator;
import org.lemurproject.galago.core.eval.aggregate.QuerySetEvaluatorFactory;
import org.lemurproject.galago.core.eval.metric.QueryEvaluator;
import org.lemurproject.galago.core.eval.metric.QueryEvaluatorFactory;
import org.lemurproject.galago.utility.Parameters;
import org.lemurproject.galago.utility.lists.Ranked;
import org.lemurproject.galago.utility.tools.AppFunction;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.*;

/**
 * 1. Create language model from hierarchy points.
 * 2. Rank language models for each question.
 * 3. ???
 * 4. Profit.
 * @author jfoley.
 */
public class RunExperiment extends AppFunction {

  @Override
  public String getName() {
    return "run-experiment";
  }

  @Override
  public String getHelpString() {
    return makeHelpStr(
      "hierarchy", "What hierarchy?",
      "questions", "Actual Questions, grouped by test."
    );
  }

  @Override
  public void run(Parameters p, PrintStream stdout) throws Exception {
    // Use defaults, then overriding args.
    Parameters argp = ExperimentCommon.DefaultParameters.clone();
    argp.copyFrom(p);

    try (ExperimentResources resources = new ExperimentResources(argp)) {
      Parameters byTestQuestions = resources.questionsByTest();
      ClassificationMethod method = ClassificationMethodFactory.create(resources);

      // run experiment on ret:
      Parameters results = runExperiment(method, byTestQuestions, argp);
      try (PrintWriter output = IO.openPrintWriter(argp.getString("output"))) {
        output.println(results.toPrettyString());
      }
    }
  }

  /** Run experiment, return results on requested metrics, aggregated by exam */
  public static Parameters runExperiment(ClassificationMethod method, Parameters byTestQuestions, Parameters argp) throws IOException {
    if(!argp.isString("output")) {
      throw new RuntimeException("Expected --output=something.json argument to save experiment details.");
    }
    Parameters resultsByExam = Parameters.create();
    Parameters resultsByQuestion = Parameters.create();
    Parameters resultsByNode = Parameters.create();
    String relevanceFeedbackMethod = argp.get("rf", "no-rf");
    boolean doRelevanceFeedback = !Objects.equals(relevanceFeedbackMethod, "no-rf");
    boolean doUniform = argp.get("make-tests-uniform", false);

    for (String exam : byTestQuestions.keySet()) {
      List<Parameters> questionsOnExam = byTestQuestions.getAsList(exam, Parameters.class);

      Map<String, List<EvalDoc>> perQuestionResults = new TreeMap<>();
      QuerySetJudgments qrels = ExperimentCommon.makeQrels(questionsOnExam);
      for (Parameters question : questionsOnExam) {
        String qid = question.getString("qid");
        System.err.println("# "+argp.getString("output")+" qid: "+qid);

        List<? extends EvalDoc> evalResults = method.predictQuestionResults(question);
        assert(evalResults != null);
        if (doRelevanceFeedback) {
          switch(relevanceFeedbackMethod) {
            case "no-rf": break;
            case "hrf":
              evalResults = hierarchyFeedback(method.resources, question.getAsList("judgments", String.class), evalResults);
              break;
            case "srf":
              evalResults = standardFeedback(method.resources.argp, question.getAsList("judgments", String.class), evalResults);
              break;
            case "hsrf":
              evalResults = hierarchyFeedback(method.resources, question.getAsList("judgments", String.class), evalResults);
              evalResults = standardFeedback(method.resources.argp, question.getAsList("judgments", String.class), evalResults);
              break;
            default:
              throw new RuntimeException(relevanceFeedbackMethod);
          }
        }
        perQuestionResults.put(qid, Checked.<List<EvalDoc>>cast(evalResults));

        Parameters scores = Parameters.create();
        for (String metric : ExperimentCommon.getMetrics(argp)) {
          QueryEvaluator qse = QueryEvaluatorFactory.create(metric, argp);
          double score = qse.evaluate(new QueryResults(Checked.<List<EvalDoc>>cast(evalResults)), qrels.get(qid));
          scores.put(metric, score);
        }


        scores.put("numResults", evalResults.size());
        resultsByQuestion.put(qid, scores);
        for (String node : question.getAsList("judgments", String.class)) {
          Parameters qscores = scores.clone();
          qscores.put("qid", qid);
          resultsByNode.extendList(node, qscores);
        }
      }

      if (doUniform) {
        Map<String, List<? extends EvalDoc>> uniformPredictions = ExamsAreUniform.attemptToLeverageUniformExamHypothesis(argp, perQuestionResults);
        perQuestionResults = Checked.cast(uniformPredictions);
      }

      Parameters scores = Parameters.create();
      for (String metric : ExperimentCommon.getMetrics(argp)) {
        QuerySetEvaluator qse = QuerySetEvaluatorFactory.create(metric, argp);
        double score = qse.evaluate(new QuerySetResults(perQuestionResults), qrels);
        scores.put(metric, score);
      }
      scores.put("numQuestions", questionsOnExam.size());

      resultsByExam.put(exam, scores);
    }
    return Parameters.parseArray(
      "argp", argp,
      "byExam", resultsByExam,
      "byQuestion", resultsByQuestion,
      "byNode", resultsByNode
    );
  }

  public static List<? extends EvalDoc> standardFeedback(Parameters argp, List<String> judgments, List<? extends EvalDoc> evalResults) {
    Set<String> positive = new HashSet<>(judgments);

    List<SimpleEvalDoc> relevant = new ArrayList<>(evalResults.size());
    List<SimpleEvalDoc> nonRelevant = new ArrayList<>(evalResults.size());

    int depth = (int) argp.get("srf-depth", 10);

    for (int i = 0; i < evalResults.size(); i++) {
      EvalDoc evalDoc = evalResults.get(i);
      // If it's in the top 10, and it's good, add it to relevant.
      int rank = i+1;
      if(rank <= depth) {
        if (positive.contains(evalDoc.getName())) {
          relevant.add(new SimpleEvalDoc(evalDoc.getName(), -1, evalDoc.getScore()));
          continue;
        }
      }
      nonRelevant.add(new SimpleEvalDoc(evalDoc.getName(), -1, evalDoc.getScore()));
    }

    List<SimpleEvalDoc> reranked = new ArrayList<>();
    reranked.addAll(relevant);
    reranked.addAll(nonRelevant);

    for (int i = 0; i < reranked.size(); i++) {
      reranked.get(i).rank = i+1;
    }

    return reranked;
  }

  public static List<? extends EvalDoc> hierarchyFeedback(ExperimentResources resources, List<String> judgments, List<? extends EvalDoc> evalResults) {
    MergedHierarchy mh = resources.getHierarchy();
    Set<String> relevantRoots = new HashSet<>();

    if(judgments.size() > 2) {
      throw new RuntimeException("Unexpected Judgments Size");
    }
    for (String judgment : judgments) {
      relevantRoots.add(mh.lookup(judgment).getRootId());
    }

    List<SimpleEvalDoc> filteredWithRelevanceFeedback = new ArrayList<>(evalResults.size());
    for (EvalDoc evalResult : evalResults) {
      String id = evalResult.getName();
      if(relevantRoots.contains(mh.lookup(id).getRootId()))
        filteredWithRelevanceFeedback.add(new SimpleEvalDoc(id, -1, evalResult.getScore()));
    }

    Ranked.setRanksByScore(filteredWithRelevanceFeedback);

    return filteredWithRelevanceFeedback;
  }

  public static void main(String[] args) throws Exception {
    AppFunction fn = new RunExperiment();
    fn.run(Parameters.parseArray(
      "output", "unsupervised.sdm.json",
      "method", "unsupervised"
    ), System.out);
  }


}
