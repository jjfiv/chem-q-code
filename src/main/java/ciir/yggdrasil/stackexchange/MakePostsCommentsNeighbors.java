package ciir.yggdrasil.stackexchange;

import org.lemurproject.galago.core.index.mem.FlushToDisk;
import org.lemurproject.galago.core.index.mem.MemoryIndex;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.tokenize.Tokenizer;
import org.lemurproject.galago.tupleflow.Processor;
import org.lemurproject.galago.utility.Parameters;
import org.lemurproject.galago.utility.tools.AppFunction;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

/**
 * @author jfoley
 */
public class MakePostsCommentsNeighbors extends AppFunction {
  @Override
  public String getName() {
    return "make-posts-comments-neighbors";
  }

  @Override
  public String getHelpString() {
    return makeHelpStr(
        "dump-folder", "Path to chemistry.stackexchange.com/ dump folder.",
        "output", "output folder to write galago index",
        "threaded", "default=false, whether to write output parts in parallel"
    );
  }

  @Override
  public void run(Parameters p, PrintStream output) throws Exception {
    String basePath = p.getString("dump-folder");
    String outputFolder = p.getString("output");
    boolean threadedWrite = p.get("threaded", false);

    List<Parameters> posts = PostParser.loadRowsFromFile(basePath + File.separator + PostParser.PostsFile);
    List<Parameters> comments = PostParser.loadRowsFromFile(basePath + File.separator + PostParser.CommentsFile);


    MemoryIndex memIndex;
    try {
      memIndex = new MemoryIndex(Parameters.parseArray("makecorpus", true));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    Tokenizer tokenizer = Tokenizer.create(Parameters.create());
    create(memIndex, posts, comments, tokenizer);
    FlushToDisk.flushMemoryIndex(memIndex, outputFolder, threadedWrite);
  }

  public static void create(Processor<Document> output, List<Parameters> posts, List<Parameters> comments, Tokenizer tokenizer) throws IOException {
    for (Parameters post : posts) {
      Document doc = new Document();
      doc.name = "Post" + post.getLong("Id");
      doc.metadata.put("Score", Long.toString(post.getLong("Score")));
      doc.text = "";
      if(post.isString("Title")) {
        doc.text += "<title>" + post.getString("Title") + "</title>";
      }
      doc.text += "<body>" + post.getString("Body") + "</body>";
      tokenizer.tokenize(doc);
      output.process(doc);
    }
    for (Parameters comment : comments) {
      Document doc = new Document();
      doc.name = "Comment" + comment.getLong("Id");
      doc.metadata.put("Score", Long.toString(comment.getLong("Score")));
      doc.text = "<body>" + comment.getString("Text") + "</body>";
      tokenizer.tokenize(doc);
      output.process(doc);
    }
  }
}
