package ciir.yggdrasil.chemistry;

import ciir.jfoley.chai.collections.util.ListFns;
import ciir.jfoley.chai.io.IO;
import ciir.jfoley.chai.string.StrUtil;
import ciir.yggdrasil.chemistry.cleaned.MHNode;
import ciir.yggdrasil.chemistry.cleaned.MergedHierarchy;
import ciir.yggdrasil.chemistry.experiments.ExperimentCommon;
import ciir.yggdrasil.util.F;
import ciir.yggdrasil.util.TSVLoader;
import org.lemurproject.galago.utility.Parameters;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author jfoley
 */
public class CreateUnifiedDataset {
  public static final String InstructionUnit = "iu";
  public static final String TestId = "test-id";
  public static final String TestQuestionId = "test-question-id";
  public static final String Judgment = "judgment";

  public static void ValidateQuestion(Parameters p) {
    if(!p.isString("qid")) throw new RuntimeException("Missing qid");
    if(!p.isString("text")) throw new RuntimeException("Missing text");
    if(!p.isString("exam")) throw new RuntimeException("Missing exam name");
    if(p.getBoolean("judged")) {
      boolean hasACCMJudgments = p.isList("accm-judgments", Integer.class);
      boolean hasUMassJudgments = p.isList("umass-judgments", Integer.class);
      if(!hasACCMJudgments && !hasUMassJudgments) throw new RuntimeException("Expected either ACCM or UMass judgments or judged=false");
    }
  }
  /** Load direct form ACS data */
  public static List<Parameters> loadACCM() {
    List<Parameters> examQs = TSVLoader.withHeader("chemistry/direct_from_company_exam_questions.tsv");

    List<Parameters> nonEmptyQs = new ArrayList<>(examQs.size());
    int qindex = 0;
    for (Parameters examQ : examQs) {
      Parameters newQ = Parameters.create();

      if (!examQ.isString("item stem")) continue;
      String stem = examQ.getString("item stem");
      if (StrUtil.compactSpaces(stem).isEmpty()) continue;

      List<Integer> judgments = new ArrayList<>();
      if (examQ.isLong("ACCM L1L2L3L4")) {
        judgments.add((int) examQ.getLong("ACCM L1L2L3L4"));
      }
      if (examQ.isLong("ACCM L1L2L3L4 secondary")) {
        judgments.add((int) examQ.getLong("ACCM L1L2L3L4 secondary"));
      }

      List<String> textKeys = Arrays.asList("item stem", "response choice A", "response choice B", "response choice C", "response choice D");
      StringBuilder sb = new StringBuilder();
      for (String textKey : textKeys) {
        sb.append(examQ.getAsString(textKey)).append('\n');
      }

      newQ.put("qid", String.format("acs-%d", qindex++));
      newQ.put("text", sb.toString());
      newQ.put("exam", examQ.get("exam name") + "_" + examQ.get("exam year"));
      if (judgments.isEmpty()) {
        newQ.put("judged", false);
      } else {
        newQ.put("judged", true);
        newQ.put("accm-judgments", judgments);
      }
      newQ.put("original", examQ);

      ValidateQuestion(newQ);

      nonEmptyQs.add(newQ);
    }

    return nonEmptyQs;
  }

  /** Load questions from UMass format dump. */
  public static List<Parameters> loadUMass() {
    List<Parameters> output = new ArrayList<>();

    try {
      for (Parameters p : OWLGeneratedQuestionLoader.load()) {

        Parameters newQ = Parameters.create();

        newQ.put("qid", String.format("umass-%d", p.getLong("id")));
        newQ.put("text", p.getString("html"));
        newQ.put("exam", String.format("umass-iu-%d", p.getLong("iu")));
        if(!p.containsKey("judgment")) {
          newQ.put("judged", false);
        } else {
          newQ.put("judged", true);
          newQ.put("umass-judgments", p.getAsList("judgment", Integer.class));
        }

        ValidateQuestion(newQ);
        output.add(newQ);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return output;
  }

  public static void main(String[] args) throws IOException {
    MergedHierarchy mh = MergedHierarchy.load(ExperimentCommon.DefaultParameters.getString("hierarchy"));

    List<Parameters> accmQs = loadACCM();
    List<Parameters> umassQs = loadUMass();

    List<Parameters> allQuestions = new ArrayList<>(ListFns.lazyConcat(accmQs, umassQs));
    List<Parameters> judgedQuestions = new ArrayList<>();
    for (Parameters q : allQuestions) {
      if(!q.get("judged", false)) continue;
      judgedQuestions.add(q);
    }

    List<Parameters> reasonableNumberPerTest = new ArrayList<>();

    // For each test
    Map<String,List<Parameters>> byTest = F.groupBy(judgedQuestions, "exam", String.class);

    Parameters byTestJSON = Parameters.create();
    System.out.println(byTest.keySet());
    for (Map.Entry<String, List<Parameters>> kv : byTest.entrySet()) {
      List<Parameters> value = kv.getValue();
      if(value.size() > 10) {
        List<Parameters> toKeep = new ArrayList<>();
        //byTestJSON.put(kv.getKey(), value);

        for (Parameters p : value) {
          List<String> judgments = new ArrayList<>();

          if (p.containsKey("accm-judgments")) {
            for (int x : p.getAsList("accm-judgments", Integer.class)) {
              MHNode node = mh.lookupACCM(x);
              if (node != null) {
                judgments.add(node.id);
              }
            }
            p.remove("accm-judgments");
          }

          if (p.containsKey("umass-judgments")) {
            for (int x : p.getAsList("umass-judgments", Integer.class)) {
              MHNode node = mh.lookupUMass(x);
              if (node != null) {
                judgments.add(node.id);
              }
            }
            p.remove("umass-judgments");
          }

          if (judgments.isEmpty()) {
            System.err.println("Missing node for judgment!");
          } else {
            reasonableNumberPerTest.add(p);
            p.put("judgments", judgments);
            p.remove("judged");
            toKeep.add(p);
          }

        }

        byTestJSON.put(kv.getKey(), toKeep);
      }
    }

    System.out.println(F.histogram(reasonableNumberPerTest, "exam", String.class));

    try(PrintWriter perTestJSONWriter = IO.openPrintWriter("per-test-questions.json")) {
      perTestJSONWriter.println(byTestJSON.toPrettyString());
    }

    System.out.println(Parameters.parseArray(
        "allQuestions", allQuestions.size(),
        "judgedQuestions", judgedQuestions.size(),
        "totalQuestions", reasonableNumberPerTest.size(),
        "numTests", F.groupBy(reasonableNumberPerTest, TestId, Integer.class).size()
    ).toPrettyString());

  }
}
