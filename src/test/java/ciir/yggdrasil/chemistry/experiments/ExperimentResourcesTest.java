package ciir.yggdrasil.chemistry.experiments;

import ciir.jfoley.chai.collections.Pair;
import ciir.jfoley.chai.collections.chained.ChaiIterable;
import ciir.jfoley.chai.collections.chained.ChaiMap;
import ciir.jfoley.chai.collections.util.ArrayFns;
import ciir.jfoley.chai.fn.Fns;
import ciir.jfoley.chai.fn.TransformFn;
import ciir.jfoley.chai.random.Sample;
import ciir.yggdrasil.chemistry.cleaned.MHNode;
import ciir.yggdrasil.chemistry.cleaned.MergedHierarchy;
import ciir.yggdrasil.stackexchange.PostParser;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.procedure.TObjectIntProcedure;
import org.junit.Test;
import org.junit.Ignore;
import org.lemurproject.galago.utility.Parameters;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ExperimentResourcesTest {
  public static boolean loud = false;

  @Test
  @Ignore("Needs data to run.")
  public void testHierarchy() throws IOException {
    try (final ExperimentResources resources = new ExperimentResources(ExperimentCommon.DefaultParameters)) {
      MergedHierarchy mh = resources.getHierarchy();

      for (MHNode mhNode : mh.getNodes()) {
        assertNotNull(mhNode.children);
        if (mhNode.parent == null) {
          assertTrue(mhNode.children.size() > 0);
          for (MHNode child : mhNode.children) {
            assertNotNull(child);
          }
        }
      }

      TObjectIntHashMap<String> nodeFrequencies = new TObjectIntHashMap<>();
      for (MHNode mhNode : mh.getNodes()) {
        if (mhNode.level == 4) {
          nodeFrequencies.put(mhNode.id, 0);
        }
      }
      for (Parameters parameters : resources.getAllQuestions()) {
        for (String nodeId : parameters.getAsList("judgments", String.class)) {
          nodeFrequencies.adjustOrPutValue(nodeId, 1, 1);
        }
      }

      ChaiIterable<Pair<Integer, Integer>> pairs = ChaiIterable.create(ArrayFns.toList(nodeFrequencies.values()))
          .groupBy(Fns.<Integer>identity()).mapValues(new TransformFn<List<Integer>, Integer>() {
            @Override
            public Integer transform(List<Integer> input) {
              return input.size();
            }
          }).pairs();

      for (Pair<Integer, Integer> pt : pairs) {
        System.out.println(pt.left + "\t" + pt.right);
      }

      if (loud) {
        nodeFrequencies.forEachEntry(new TObjectIntProcedure<String>() {
          @Override
          public boolean execute(String a, int b) {
            String c = resources.getHierarchy().lookup(a).getRootRomanName();
            String t = resources.getHierarchy().lookup(a).description;
            System.out.println(a + "\t" + b + "\t" + c + "\t" + t);
            return true;
          }
        });
      }
    }
  }

  @Test
  @Ignore("Needs data to run.")
  public void testGetHierarchyCounts() throws IOException {
    try (final ExperimentResources resources = new ExperimentResources(ExperimentCommon.DefaultParameters)) {
      MergedHierarchy mh = resources.getHierarchy();

      TIntIntHashMap levelCounts = new TIntIntHashMap();
      TIntIntHashMap childrenCounts = new TIntIntHashMap();

      int sum = 0;
      int total = 0;
      for (MHNode mhNode : mh.getNodes()) {
        if (mhNode.level == 1)
          System.out.println(mhNode.title);
        if (mhNode.level == 1)
          System.out.println(mhNode.description);
        levelCounts.adjustOrPutValue(mhNode.level, 1, 1);
        childrenCounts.adjustOrPutValue(mhNode.children.size(), 1, 1);
        ++total;

        sum += new HashSet<>(resources.tokenizer.tokenize(mhNode.description).terms).size();
      }

      System.out.println(Parameters.parseArray("total", total, "avgLength", ((double) sum) / ((double) total),
          "levelCounts", levelCounts.toString(), "childrenCounts", childrenCounts.toString()));
    }
  }

  @Test
  @Ignore("Needs data to run.")
  public void dumpRandomStackOverflowQuestion() {
    String basePath = "/home/jfoley/Downloads/chemistry.stackexchange.com/";
    List<Parameters> posts = PostParser.loadRowsFromFile(basePath + File.separator + PostParser.PostsFile);

    for (Parameters parameters : Sample.byRandomWeight(posts, 10, new Random(13))) {
      System.out.println(parameters);
    }
  }

}