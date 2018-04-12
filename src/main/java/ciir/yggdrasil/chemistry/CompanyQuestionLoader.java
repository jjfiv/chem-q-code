package ciir.yggdrasil.chemistry;

import ciir.jfoley.chai.string.StrUtil;
import ciir.yggdrasil.util.TSVLoader;
import org.lemurproject.galago.utility.Parameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jfoley
 */
public class CompanyQuestionLoader {

  public static Map<Object,Integer> countBy(Iterable<Parameters> dataset, String key) {
    Map<Object,Integer> counted = new HashMap<>();
    for (Parameters parameters : dataset) {
      Object val = parameters.get(key);
      int before = 0;
      Integer prev = counted.get(val);
      if(prev != null) {
        before = prev;
      }
      counted.put(val, before+1);
    }
    return counted;
  }

  public static List<Parameters> load() {
    List<Parameters> examQs = TSVLoader.withHeader("chemistry/direct_from_company_exam_questions.tsv");

    List<Parameters> nonEmptyQs = new ArrayList<>(examQs.size());
    for (Parameters examQ : examQs) {
      if(!examQ.isString("item stem")) continue;
      String stem = examQ.getString("item stem");
      if(StrUtil.compactSpaces(stem).isEmpty()) continue;

      List<Integer> judgments = new ArrayList<>();
      if(examQ.isLong("ACCM L1L2L3L4")) {
        judgments.add((int) examQ.getLong("ACCM L1L2L3L4"));
      }
      if(examQ.isLong("ACCM L1L2L3L4 secondary")) {
        judgments.add((int) examQ.getLong("ACCM L1L2L3L4 secondary"));
      }
      examQ.put("iu", examQ.get("exam name")+"_"+examQ.get("exam year"));
      examQ.put("judgment", judgments);
      nonEmptyQs.add(examQ);
    }

    return examQs;
  }

  public static void main(String[] args) {
    List<Parameters> examQs = load();

    List<Parameters> nonEmptyQs = new ArrayList<>(examQs.size());
    for (Parameters examQ : examQs) {
      if(!examQ.isString("item stem")) continue;
      String stem = examQ.getString("item stem");
      if(StrUtil.compactSpaces(stem).isEmpty()) continue;
      nonEmptyQs.add(examQ);

      if(examQ.get("Correct Answer").equals("V")) {
        System.err.println(examQ);
      }
    }

    System.out.println("Complexity: "+countBy(nonEmptyQs, "Complexity 1,2,3"));
    System.out.println("Published Rubric Complexity: "+countBy(nonEmptyQs, "complexity"));
    System.out.println("Difficulty: (Classical Test Theory):"+countBy(nonEmptyQs, "item diff"));
    System.out.println("Discrimination (Classical Test Theory): "+countBy(nonEmptyQs, "item discr"));

    System.out.println("Hierarchy Label: "+countBy(nonEmptyQs, "ACCM L1L2L3L4"));
    System.out.println("Hierarchy Label 2: "+countBy(nonEmptyQs, "ACCM L1L2L3L4 secondary"));

    System.out.println("Correct Answer: "+countBy(nonEmptyQs, "Correct Answer"));
    System.out.println("exam year: "+countBy(nonEmptyQs, "exam year"));


    System.out.println("ACR (Algorithmic, Conceptual, Recall): "+countBy(nonEmptyQs, "ACR"));
    System.out.println("visual component "+countBy(nonEmptyQs, "visual component"));

    System.out.println("nonEmptyQs: "+nonEmptyQs.size());


  }
}
