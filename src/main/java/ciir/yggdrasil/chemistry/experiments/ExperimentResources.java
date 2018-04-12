package ciir.yggdrasil.chemistry.experiments;

import ciir.jfoley.chai.collections.util.ListFns;
import ciir.jfoley.chai.random.Sample;
import ciir.yggdrasil.chemistry.cleaned.MHNode;
import ciir.yggdrasil.chemistry.cleaned.MergedHierarchy;
import ciir.yggdrasil.galago.TemporaryGalagoIndex;
import ciir.yggdrasil.stackexchange.MakePostsCommentsNeighbors;
import ciir.yggdrasil.stackexchange.PostParser;
import org.lemurproject.galago.core.index.mem.MemoryIndex;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.retrieval.query.Node;
import org.lemurproject.galago.core.tokenize.Tokenizer;
import org.lemurproject.galago.core.util.WordLists;
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.utility.Parameters;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author jfoley.
 */
public class ExperimentResources implements Closeable {
  private Set<String> stopwords;
  public Tokenizer tokenizer;
  public final Parameters argp;
  public MergedHierarchy hierarchy;
  public Parameters questionsByTest;

  public TemporaryGalagoIndex hierarchyLM = null;
  public TemporaryGalagoIndex questionLM = null;
  private TemporaryGalagoIndex qnLM = null;
  public List<Closeable> closeMe = new ArrayList<>();

  public ExperimentResources(Parameters argp) throws IOException {
    this.argp = argp;
    this.tokenizer = Tokenizer.create(argp);
    this.hierarchy = MergedHierarchy.load(argp.getString("hierarchy"));
    this.questionsByTest = Parameters.parseFile(argp.getString("questions"));
    this.stopwords = Collections.emptySet();
    if(argp.get("stopwords", false)) {
      this.stopwords = WordLists.getWordList("inquery");
    }
  }

  public Map<String, Set<String>> getExamSplitInfo() {

    Parameters qs = questionsByTest();
    Set<String> allExams = qs.keySet();
    Map<String, Set<String>> otherExams = new HashMap<>();
    for (String exam : allExams) {
      Set<String> clone = new HashSet<>(allExams);
      clone.remove(exam);
      otherExams.put(exam, clone);
    }
    if(!argp.isLong("downsample")) return otherExams;

    // downsampling...
    int downsample = (int) argp.getLong("downsample");
    int seed = (int) argp.get("seed", 13);
    Random rand = new Random(seed);

    Map<String,Set<String>> downsampled = new HashMap<>();
    for (Map.Entry<String, Set<String>> kv : otherExams.entrySet()) {
      String key = kv.getKey();
      Set<String> newVals = new HashSet<>();
      newVals.addAll(Sample.byRandomWeight(kv.getValue(), downsample, rand));
      downsampled.put(key, newVals);
    }

    return downsampled;
  }

  public Set<String> getStopwords() {
    return stopwords;
  }

  public MergedHierarchy getHierarchy() {
    return hierarchy;
  }

  public Parameters questionsByTest() {
    return questionsByTest;
  }

  public TemporaryGalagoIndex getHierarchyLM() {
    if(hierarchyLM  == null) {
      try {
        MemoryIndex index = TemporaryGalagoIndex.memoryIndexBuilder();
        for (MHNode mhNode : getHierarchy().getNodes()) {
          Document doc = new Document();
          doc.text = mhNode.title + '\n' + mhNode.description + '\n';
          tokenizer.tokenize(doc);
          doc.name = mhNode.id;

          index.process(doc);
        }

        hierarchyLM = new TemporaryGalagoIndex(index);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    return hierarchyLM;
  }

  public List<Parameters> getAllQuestions() {
    List<Parameters> output = new ArrayList<>();
    for (Object o : questionsByTest.values()) {
      List<Parameters> forTest = (List<Parameters>) o;
      output.addAll(forTest);
    }
    return output;
  }

  public TemporaryGalagoIndex getQuestionLM() {
    if(questionLM == null) {
      try {
        MemoryIndex index = TemporaryGalagoIndex.memoryIndexBuilder();
        generateQuestionsAsDocuments(index);
        questionLM = new TemporaryGalagoIndex(index);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return questionLM;
  }

  public TemporaryGalagoIndex getExpansionLM() {
    //TODO switch by type.
    try {
      String basePath = "/home/jfoley/Downloads/chemistry.stackexchange.com/";
      List<Parameters> posts = PostParser.loadRowsFromFile(basePath + File.separator + PostParser.PostsFile);
      List<Parameters> comments = PostParser.loadRowsFromFile(basePath + File.separator + PostParser.CommentsFile);

      MemoryIndex bldr = TemporaryGalagoIndex.memoryIndexBuilder();
      MakePostsCommentsNeighbors.create(bldr, posts, comments, tokenizer);
      return buildAndCloseLater(bldr);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Node buildQuery(List<String> terms) {
    Node query = new Node(argp.get("retrievalModel", "sdm"));

    for (String term : terms) {
      if(getStopwords().contains(term)) continue;
      query.addChild(Node.Text(term));
    }

    return query;
  }

  @Override
  public void close() throws IOException {
    List<Closeable> resources = ListFns.lazyConcat(closeMe,
        Arrays.<Closeable>asList(
            hierarchyLM,
            questionLM,
            qnLM
        ));
    for (Closeable obj : resources) {
      if(obj != null) {
        obj.close();
      }
    }
  }

  /** Create documents from questions */
  public void generateQuestionsAsDocuments(Processor<Document> output) throws IOException {
    for (Parameters question : getAllQuestions()) {
      // Put a bunch of info into the document name:
      String name = Parameters.parseArray(
        "qid", question.getString("qid"),
        "exam", question.getString("exam"),
        "judgments", question.getAsList("judgments", String.class)
      ).toString();

      // Make Galago document:
      Document doc = new Document();
      doc.name = name;
      doc.text = question.getString("text");
      tokenizer.tokenize(doc);

      output.process(doc);
    }
  }

  /** Create documents from questions */
  void generateNodesAsQuestionDocs(Processor<Document> output) throws IOException {
    for (MHNode mhNode : getHierarchy().getNodes()) {
      // Put a bunch of info into the document name:
      String name = Parameters.parseArray(
        "qid", mhNode.id,
        "exam", "# hierarchy",
        "judgments", Collections.singletonList(mhNode.id)
      ).toString();

      // Make Galago document:
      Document doc = new Document();
      doc.name = name;
      doc.text = mhNode.title + '\n' + mhNode.description + '\n';
      tokenizer.tokenize(doc);

      output.process(doc);
    }
  }

  public TemporaryGalagoIndex getQuestionsAndNodesLM() {
      if(qnLM == null) {
        try {
          MemoryIndex index = TemporaryGalagoIndex.memoryIndexBuilder();
          generateQuestionsAsDocuments(index);
          generateNodesAsQuestionDocs(index);
          qnLM = new TemporaryGalagoIndex(index);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
      return qnLM;
    }

  /** Turn a MemoryIndex into a TemporaryGalagoIndex and close it when this class gets closed. */
  public TemporaryGalagoIndex buildAndCloseLater(MemoryIndex memoryIndex) {
    try {
      TemporaryGalagoIndex index = new TemporaryGalagoIndex(memoryIndex);
      closeMe.add(index);
      return index;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public Node buildQuery(String text) {
    return buildQuery(tokenizer.tokenize(text).terms);
  }
}
