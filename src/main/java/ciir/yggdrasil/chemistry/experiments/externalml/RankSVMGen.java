package ciir.yggdrasil.chemistry.experiments.externalml;

import ciir.jfoley.chai.collections.util.MapFns;
import ciir.jfoley.chai.collections.util.SetFns;
import ciir.jfoley.chai.io.IO;
import ciir.yggdrasil.chemistry.cleaned.MHNode;
import ciir.yggdrasil.chemistry.experiments.ClassificationMethod;
import ciir.yggdrasil.chemistry.experiments.ExperimentCommon;
import ciir.yggdrasil.chemistry.experiments.ExperimentResources;
import ciir.yggdrasil.chemistry.experiments.method.HierarchicalSDM;
import ciir.yggdrasil.galago.TemporaryGalagoIndex;
import gnu.trove.map.hash.TIntDoubleHashMap;
import org.lemurproject.galago.core.index.mem.MemoryIndex;
import org.lemurproject.galago.core.retrieval.LocalRetrieval;
import org.lemurproject.galago.core.retrieval.ScoredDocument;
import org.lemurproject.galago.core.retrieval.query.Node;
import org.lemurproject.galago.utility.Parameters;
import org.lemurproject.galago.utility.tools.AppFunction;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.*;

/**
 * @author jfoley.
 */
public class RankSVMGen extends AppFunction {
  @Override
  public String getName() {
    return "ranksvm-gen";
  }

  @Override
  public String getHelpString() {
    return makeHelpStr("defaults", "etc...");
  }

  @Override
  public void run(Parameters p, PrintStream stdout) throws Exception {
    Parameters argp = p.clone();
    argp.copyFrom(ExperimentCommon.DefaultParameters);

    List<String> allIds = new ArrayList<>();
    Map<String, Integer> nodeIdsToFeatureIds = new HashMap<>();

    Map<String, List<String>> ptsBySplit = new HashMap<>();

    try (ExperimentResources resources = new ExperimentResources(argp)) {

      Map<String, Set<String>> nodeData = new HashMap<>();

      // Build language models for each node.
      // Build a new index.
      MemoryIndex memoryIndex = TemporaryGalagoIndex.memoryIndexBuilder();
      for (MHNode mhNode : resources.getHierarchy().getNodes()) {
        allIds.add(mhNode.id);
        nodeIdsToFeatureIds.put(mhNode.id, allIds.size());
        //memoryIndex.process(HierarchicalSDM.makeDocument(resources, mhNode, mhNode.descendants(), 0, null));
        memoryIndex.process(HierarchicalSDM.makeDocument(resources, mhNode, Collections.singletonList(mhNode), 0, null));
        nodeData.put(mhNode.id, new HashSet<>(resources.tokenizer.tokenize(mhNode.title + ' ' + mhNode.description).terms));
      }
      LocalRetrieval ret = resources.buildAndCloseLater(memoryIndex).getRetrieval();

      // Generate features for all questions.
      List<Parameters> allQuestions = resources.getAllQuestions();
      for (int qi = 0; qi < allQuestions.size(); qi++) {
        System.err.println(qi);
        Parameters question = allQuestions.get(qi);

        Set<String> qterms = new HashSet<>(resources.tokenizer.tokenize(question.getString("text")).terms);
        //String qid = question.getString("qid"); // TODO save this mapping to qi
        Set<String> relevantFids = new HashSet<>();
        for (String nodeId : question.getAsList("judgments", String.class)) {
          relevantFids.add(nodeId);
        }

        Node query = ClassificationMethod.buildQueryFromQuestion(resources, question);
        for (ScoredDocument sdoc : ExperimentCommon.runWorkingSetQuery(ret, query, allIds)) {
          MHNode node = resources.getHierarchy().lookup(sdoc.getName());
          // relevant or not.
          int documentClass = 0;
          if (relevantFids.contains(sdoc.getName())) {
            documentClass = 1;
          }
          // features.
          final StringBuilder svmf = new StringBuilder();
          // class
          svmf.append(documentClass).append(' ');
          // qid
          svmf.append("qid:").append(qi).append(' ');

          TIntDoubleHashMap features = new TIntDoubleHashMap();
          features.put(1, sdoc.getScore()); // H-SDM feature...
          features.put(2, 1.0 / sdoc.getRank());
          features.put(3, node.level);
          features.put(4, node.title.length());
          features.put(5, node.description.length());
          features.put(6, node.descendants().size());

          Set<String> nterms = nodeData.get(node.id);
          double isect = SetFns.intersection(qterms, nterms).size();
          double union = SetFns.union(qterms, nterms).size();
          features.put(7, isect);
          features.put(8, union);
          // jaccard
          features.put(9, isect / union);
          // dice
          features.put(10, 2 * (isect / union) / (qterms.size() + nterms.size()));

          for (int fid = 1; fid <= 10; fid++) {
            double val = features.get(fid);
            svmf.append(fid).append(':').append(val).append(' ');
          }

          svmf.append("# ").append(node.id);

          String examName = question.getString("exam");
          int id = examName.hashCode() % 20;
          String split;
          if(id < 10) {
            split = "sdm-train";
          } else if(id < 15) {
            split = "sdm-validate";
          } else {
            split = "sdm-eval";
          }
          MapFns.extendListInMap(ptsBySplit, split, svmf.toString());
        }
      }

    }

    // Write by split.
    for (String split : ptsBySplit.keySet()) {
      System.err.println("Writing "+split+".svm");
      try (PrintWriter splitSVM = IO.openPrintWriter(split + ".svm")) {
        for (String data : ptsBySplit.get(split)) {
          splitSVM.println(data);
        }
      }
    }

    System.err.println("Finished!");
  }

  public static void main(String[] args) throws Exception {
    Parameters argp = Parameters.parseArgs(args);
    AppFunction fn = new RankSVMGen();
    fn.run(argp, System.out);
  }
}
