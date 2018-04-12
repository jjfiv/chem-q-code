package ciir.yggdrasil.chemistry;

import ciir.jfoley.chai.collections.util.MapFns;
import ciir.yggdrasil.util.TSVLoader;
import org.lemurproject.galago.utility.Parameters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jfoley.
 */
public class JudgmentLoader {
  public static Map<Integer, List<Integer>> load(String... files) {
    Map<Integer, List<Integer>> questionToCategoryMapping = new HashMap<>();

    for (String file : files) {
      for (Parameters row : TSVLoader.withHeader(file)) {
        int qid = (int) row.get("QuestionID", -1l);
        int fw3 = (int) row.get("FrameworkCategoryID3", -1l);
        int fw4 = (int) row.get("FrameworkCategoryID4", -1l);
        if(qid == -1) continue;
        if(fw3 != -1) {
          MapFns.extendListInMap(questionToCategoryMapping, qid, fw3);
        }
        if(fw4 != -1) {
          MapFns.extendListInMap(questionToCategoryMapping, qid, fw4);
        }
      }
    }

    return questionToCategoryMapping;
  }
}
