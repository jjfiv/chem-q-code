package ciir.yggdrasil.chemistry.experiments.method;

import ciir.jfoley.chai.collections.util.IterableFns;
import ciir.jfoley.chai.collections.util.MapFns;
import ciir.jfoley.chai.io.IO;
import ciir.jfoley.chai.io.LinesIterable;
import ciir.jfoley.chai.Spawn;
import ciir.yggdrasil.chemistry.cleaned.MHNode;
import ciir.yggdrasil.chemistry.experiments.ClassificationMethod;
import ciir.yggdrasil.chemistry.experiments.ExperimentCommon;
import ciir.yggdrasil.chemistry.experiments.ExperimentResources;
import ciir.yggdrasil.chemistry.experiments.externalml.LibSVMGen;
import org.lemurproject.galago.core.eval.EvalDoc;
import org.lemurproject.galago.core.eval.SimpleEvalDoc;
import org.lemurproject.galago.tupleflow.FileUtility;
import org.lemurproject.galago.utility.FSUtil;
import org.lemurproject.galago.utility.Parameters;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * @author jfoley.
 */
public class LibLinearClassifier extends ClassificationMethod {

  private final Map<String,String> predictedClassForQuestion;

  public LibLinearClassifier(ExperimentResources resources) throws IOException, InterruptedException {
    super(resources);

    Parameters argp = resources.argp;
    Map<String, Set<String>> examSplitInfo = resources.getExamSplitInfo();

    // Collect integer ids for classes
    Map<String, Integer> nodeToClass = new HashMap<>();
    Map<Integer, String> classToNode = new HashMap<>();
    int classNum = 0;
    for (MHNode mhNode : resources.getHierarchy().getNodes()) {
      classToNode.put(classNum, mhNode.id);
      nodeToClass.put(mhNode.id, classNum);
      // increment, continue;
      classNum++;
    }

    // Collect training instances by split:
    Map<String, List<String>> trainPointsBySplit = new HashMap<>();
    Map<String, List<String>> evalPointsBySplit = new HashMap<>();
    Map<String, List<String>> qidsBySplit = new HashMap<>();

    Map<String, Set<String>> expectedLabelForQuestion = new HashMap<>();

    // Generate features, collect.
    for (Parameters question : resources.getAllQuestions()) {
      List<String> qterms = resources.tokenizer.tokenize(question.getString("text")).terms;
      List<Map.Entry<Integer,Double>> features = IterableFns.sorted(LibSVMGen.generateFeatures(qterms).entrySet(), new Comparator<Map.Entry<Integer, Double>>() {
        @Override
        public int compare(Map.Entry<Integer, Double> lhs, Map.Entry<Integer, Double> rhs) {
          return Integer.compare(lhs.getKey(), rhs.getKey());
        }
      });

      // save judgments
      expectedLabelForQuestion.put(question.getString("qid"), new HashSet<>(question.getAsList("judgments", String.class)));
      //System.out.println(question.getString("qid")+" : "+expectedLabelForQuestion.get(question.getString("qid")));

      for (String nodeId : question.getAsList("judgments", String.class)) {
        int label = nodeToClass.get(nodeId);
        // features.
        final StringBuilder svmf = new StringBuilder();
        // class
        svmf.append(label).append(' ');

        for (Map.Entry<Integer, Double> kv : features) {
          svmf.append(kv.getKey()).append(':').append(kv.getValue()).append(' ');
        }

        String learningString = svmf.toString();

        String mySplit = question.getString("exam");
        MapFns.extendListInMap(evalPointsBySplit, mySplit, learningString);
        MapFns.extendListInMap(qidsBySplit, mySplit, question.getString("qid"));
        for (String split : examSplitInfo.get(mySplit)) {
          MapFns.extendListInMap(trainPointsBySplit, split, learningString);
        }
      }
    }

    File trainingDir = FileUtility.createTemporaryDirectory();
    File evaluationDir = FileUtility.createTemporaryDirectory();
    File outputDir = FileUtility.createTemporaryDirectory();

    resources.closeMe.add(new DirectoryDeleter(trainingDir));
    resources.closeMe.add(new DirectoryDeleter(evaluationDir));
    resources.closeMe.add(new DirectoryDeleter(outputDir));

    this.predictedClassForQuestion = new HashMap<>();
    String liblinearTrain = argp.get("liblinear-train", "/home/jfoley/bin/liblinear-1.96/train");
    String liblinearPredict = argp.get("liblinear-predict", "/home/jfoley/bin/liblinear-1.96/predict");

    // Save training instances to temporary files by split:
    for (Map.Entry<String, List<String>> kv : trainPointsBySplit.entrySet()) {
      String splitName = kv.getKey();
      List<String> trainPoints = kv.getValue();
      List<String> evalPoints = evalPointsBySplit.get(splitName);
      List<String> qids = qidsBySplit.get(splitName);

      String trainFile = String.format("%s/%s.svm", trainingDir.getAbsolutePath(), splitName);
      String modelFile = String.format("%s/%s.model", trainingDir.getAbsolutePath(), splitName);
      String evalFile = String.format("%s/%s.svm", evaluationDir.getAbsolutePath(), splitName);
      String outFile = String.format("%s/%s.out", outputDir.getAbsolutePath(), splitName);
      try( PrintWriter trainW = IO.openPrintWriter(trainFile)) {
        for (String dataPoint : trainPoints) {
          trainW.println(dataPoint);
        }
      }
      try( PrintWriter trainW = IO.openPrintWriter(evalFile)) {
        for (String dataPoint : evalPoints) {
          trainW.println(dataPoint);
        }
      }

      Spawn.doProcess(liblinearTrain, "-c", "0.02", "-s", "4", trainFile, modelFile);
      Spawn.doProcess(liblinearPredict, evalFile, modelFile, outFile);


      List<String> labelStrings = IterableFns.intoList(LinesIterable.fromFile(outFile));
      int accurate = 0;
      int total = 0;
      for (int i = 0; i < labelStrings.size(); i++) {
        String qid = qids.get(i);
        if(i > 0 && qid.equals(qids.get(i-1))) continue;
        int label = Integer.parseInt(labelStrings.get(i));
        String nodeId = classToNode.get(label);
        assert(nodeId != null);
        this.predictedClassForQuestion.put(qid, nodeId);
        Set<String> expected = expectedLabelForQuestion.get(qid);
        total++;
        if(expected.contains(nodeId)) {
          accurate++;
        }
        System.out.println(Parameters.parseArray("qid", qid, "i", i, "label", label, "nodeId", nodeId, "expected", new ArrayList<>(expected)));
      }

      System.out.println("Predictions correct: "+accurate+" of "+total);
    }
    assert(!this.predictedClassForQuestion.isEmpty());
  } // end constructor

  public static class DirectoryDeleter implements Closeable {
    private final File tmp;

    DirectoryDeleter(File tmp) {
      assert(tmp != null);
      assert(tmp.exists());
      assert(tmp.isDirectory());
      this.tmp = tmp;
    }

    @Override
    public void close() throws IOException {
      FSUtil.deleteDirectory(this.tmp);
    }
  }

  @Override
  public String getName() {
    return "ranksvm";
  }

  @Override
  public List<? extends EvalDoc> predictQuestionResults(Parameters question) {
    String qid = question.getString("qid");
    String nodeId = this.predictedClassForQuestion.get(qid);
    return Collections.singletonList(new SimpleEvalDoc(nodeId, 1, 1.0));
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    try(ExperimentResources resources = new ExperimentResources(ExperimentCommon.DefaultParameters)) {
      LibLinearClassifier cl = new LibLinearClassifier(resources);
      System.out.println(cl.predictedClassForQuestion);
    }
  }
}
