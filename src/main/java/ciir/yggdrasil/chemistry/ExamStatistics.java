package ciir.yggdrasil.chemistry;

import ciir.jfoley.chai.collections.util.MapFns;
import ciir.yggdrasil.util.TSVLoader;
import ciir.yggdrasil.util.F;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.hash.TIntHashSet;
import org.lemurproject.galago.utility.Parameters;

import java.io.IOException;
import java.util.*;

/**
 * @author jfoley
 */
public class ExamStatistics {

  public static void main(String[] args) throws IOException {
    //List<Parameters> questions = OWLGeneratedQuestionLoader.load();
    List<Parameters> questions = CompanyQuestionLoader.load();
    List<Parameters> rows = TSVLoader.withHeader("chemistry/flat-framework.tsv");



    //System.out.println(rows.get(0).toPrettyString());
    //System.out.println(questions.get(0).toPrettyString());

    Set<String> level1titles = new HashSet<>();
    Set<String> level2titles = new HashSet<>();
    TIntHashSet level1set = new TIntHashSet();
    TIntHashSet level2set = new TIntHashSet();
    TIntHashSet level3set = new TIntHashSet();
    TIntHashSet level4set = new TIntHashSet();
    TIntIntHashMap level1Mappings = new TIntIntHashMap();
    TIntIntHashMap level2Mappings = new TIntIntHashMap();
    TIntIntHashMap level3Mappings = new TIntIntHashMap();

    TIntIntHashMap level3childCount = new TIntIntHashMap();

    for (Parameters row : rows) {
      int id4 = (int) row.getLong("FrameworkCategoryID4");
      int id3 = (int) row.getLong("FrameworkCategoryID3");
      int id2 = (int) row.getLong("FrameworkCategoryID2");
      int id1 = (int) row.getLong("FrameworkCategoryID1");

      level1titles.add(row.getString("Level1Title"));
      level2titles.add(row.getString("Level2Description"));

      level1set.add(id1);
      level2set.add(id2);
      level3set.add(id3);
      level4set.add(id4);

      level3childCount.adjustOrPutValue(id3, 1, 1);

      level1Mappings.put(id4, id1);
      level1Mappings.put(id3, id1);
      level1Mappings.put(id2, id1);
      level1Mappings.put(id1, id1);

      level2Mappings.put(id4, id2);
      level2Mappings.put(id3, id2);
      level2Mappings.put(id2, id2);

      level3Mappings.put(id4, id3);
      level3Mappings.put(id3, id3);
    }

    //System.err.println(level1titles);
    //System.err.println(level2titles);

    TIntIntHashMap histOfLeafSizes = new TIntIntHashMap();
    for (int i : level3childCount.values()) {
      histOfLeafSizes.adjustOrPutValue(i, 1,1);
    }

    System.out.println(histOfLeafSizes);

    ;
    Map<Object, List<Parameters>> byTest = F.groupBy(questions, CreateUnifiedDataset.InstructionUnit, Object.class);
    Map<Object, List<Parameters>> judgedTests = new HashMap<>();
    for (Parameters question : questions) {
      Object instructionUnit = question.get(CreateUnifiedDataset.InstructionUnit);
      if(question.containsKey(CreateUnifiedDataset.Judgment)) {
        MapFns.extendListInMap(judgedTests, instructionUnit, question);
      }
    }

    System.out.println("Total Exams: "+byTest.size());
    System.out.println("Total Judged Exams: " + judgedTests.size());

    for (Object iu : judgedTests.keySet()) {
      List<Parameters> jq = judgedTests.get(iu);
      TIntIntHashMap level4categories = new TIntIntHashMap();
      TIntIntHashMap level3categories = new TIntIntHashMap();
      TIntIntHashMap level2categories = new TIntIntHashMap();
      TIntIntHashMap level1categories = new TIntIntHashMap();
      for (Parameters qjson : jq) {
        for (int judgment  : qjson.getAsList(CreateUnifiedDataset.Judgment, Integer.class)) {
          int category = (int) judgment;
          level4categories.adjustOrPutValue(category, 1, 1);
          level3categories.adjustOrPutValue(level3Mappings.get(category), 1, 1);
          level2categories.adjustOrPutValue(level2Mappings.get(category), 1, 1);
          level1categories.adjustOrPutValue(level1Mappings.get(category), 1, 1);
        }
      }

      System.out.printf("Test %s: %d / %d judged\n", iu, jq.size(), byTest.get(iu).size());
      System.out.printf("       : Level4: %3d. Level 3: %3d. Level 2: %3d. Level 1: %3d\n", level4categories.size(), level3categories.size(), level2categories.size(), level1categories.size());
      System.out.printf(" out of: Level4: %3d. Level 3: %3d. Level 2: %3d. Level 1: %3d\n", level4set.size(), level3set.size(), level2set.size(), level1set.size());

      //System.out.println(level1categories.valueCollection());
      //System.out.println(level2categories.valueCollection());
      //System.out.println(level3categories.valueCollection());
      //System.out.println(level4categories.valueCollection());
    }

  }
}
