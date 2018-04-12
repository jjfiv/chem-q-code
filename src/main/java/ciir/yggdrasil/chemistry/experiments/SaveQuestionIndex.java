package ciir.yggdrasil.chemistry.experiments;

import ciir.yggdrasil.galago.TemporaryGalagoIndex;
import org.lemurproject.galago.core.index.mem.FlushToDisk;
import org.lemurproject.galago.core.index.mem.MemoryIndex;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.utility.Parameters;
import org.lemurproject.galago.utility.tools.AppFunction;

import java.io.PrintStream;

/**
 * @author jfoley
 */
public class SaveQuestionIndex extends AppFunction {
  @Override
  public String getName() {
    return "save-question-index";
  }

  @Override
  public String getHelpString() {
    return makeHelpStr(
        "defaults...", "as in run-experiment",
        "output", "output directory to contain index."
    );
  }

  @Override
  public void run(Parameters p, PrintStream printStream) throws Exception {
    Parameters argp = ExperimentCommon.DefaultParameters.clone();
    argp.copyFrom(p);

    try (ExperimentResources resources = new ExperimentResources(argp)) {
      MemoryIndex memoryIndex = TemporaryGalagoIndex.memoryIndexBuilder();
      for (Parameters question : resources.getAllQuestions()) {

        // Make Galago document:
        Document doc = new Document();
        doc.name = question.getString("qid");
        doc.text = question.getString("text");
        doc.metadata.put("exam", question.getString("exam"));
        doc.metadata.put("judgments", question.get("judgments").toString());
        resources.tokenizer.tokenize(doc);

        memoryIndex.process(doc);
      }

      FlushToDisk.flushMemoryIndex(memoryIndex, argp.getString("output"));
    }
  }

  public static void main(String[] args) throws Exception {
    AppFunction fn = new SaveQuestionIndex();
    fn.run(Parameters.parseArray("output", "questions.galago"), System.out);
  }
}
