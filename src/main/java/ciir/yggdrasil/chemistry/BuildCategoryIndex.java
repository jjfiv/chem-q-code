package ciir.yggdrasil.chemistry;

import ciir.jfoley.chai.collections.util.ListFns;
import ciir.jfoley.chai.string.StrUtil;
import ciir.yggdrasil.util.TSVLoader;
import ciir.jfoley.chai.errors.NotHandledNow;
import org.lemurproject.galago.core.index.mem.FlushToDisk;
import org.lemurproject.galago.core.index.mem.MemoryIndex;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.retrieval.LocalRetrieval;
import org.lemurproject.galago.core.retrieval.Results;
import org.lemurproject.galago.core.retrieval.ScoredDocument;
import org.lemurproject.galago.core.retrieval.query.Node;
import org.lemurproject.galago.core.tokenize.Tokenizer;
import org.lemurproject.galago.utility.Parameters;
import org.lemurproject.galago.utility.tools.AppFunction;

import java.io.PrintStream;
import java.util.List;
import java.util.Objects;

/**
 * @author jfoley.
 */
public class BuildCategoryIndex extends AppFunction {

  // Create an in-memory index.
  public static MemoryIndex loadCategoriesInMemory(String inputFramework, Parameters argp) throws Exception {
    List<Parameters> rows = TSVLoader.withHeader(inputFramework);
    System.out.println(rows.get(0).keySet());

    LocalRetrieval ret = null;
    if(Objects.equals(argp.get("method", "squash"), "expand")) {
      ret = new LocalRetrieval(argp.getString("expansionIndex"));
    }

    MemoryIndex memIndex;
    try {
      memIndex = new MemoryIndex(Parameters.parseArray("makecorpus", true));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    Tokenizer tokenizer = Tokenizer.create(Parameters.create());

    // Turn each row into its own document:
    for (Parameters row : rows) {
      Document doc = new Document();
      doc.name = "C"+row.getLong("FrameworkCategoryID4");

      doc.metadata.put("id4", Long.toString(row.getLong("FrameworkCategoryID4")));
      doc.metadata.put("id3", Long.toString(row.getLong("FrameworkCategoryID3")));
      doc.metadata.put("id2", Long.toString(row.getLong("FrameworkCategoryID2")));
      doc.metadata.put("id1", Long.toString(row.getLong("FrameworkCategoryID1")));

      doc.metadata.put("title4", row.getString("Level4Title"));
      doc.metadata.put("title3", row.getString("Level3Title"));
      doc.metadata.put("title2", row.getString("Level2Title"));
      doc.metadata.put("title1", row.getString("Level1Title"));

      doc.text = makeDocumentText(row, ret, argp);
      tokenizer.tokenize(doc);
      memIndex.process(doc);
    }

    return memIndex;
  }

  public static String makeDocumentText(Parameters row, LocalRetrieval ret, Parameters argp) throws Exception {
    String method = argp.get("method", "squash");
    switch (method) {
      case "squash":
        return row.getString("Level4Description") + "\n" +
          row.getString("Level3Description") + "\n" +
          row.getString("Level2Description") + "\n" +
          row.getString("Level1Description") + "\n";
      case "level4":
        return row.getString("Level4Description");
      case "expand":
        String query = row.getString("Level4Description");

        Node nq = new Node("combine");
        for (String s : StrUtil.pretendTokenize(query)) {
          nq.addChild(Node.Text(s));
        }
        Parameters qp = Parameters.create();
        Node xq = ret.transformQuery(nq, qp);
        Results res = ret.executeQuery(xq, qp);

        StringBuilder text = new StringBuilder();
        text.append("<base>").append(query).append("</base>\n");
        for (ScoredDocument expansionDoc : ListFns.take(res.scoredDocuments, (int) argp.get("expansionDocs", 2))) {
          Document doc = ret.getDocument(expansionDoc.documentName, Document.DocumentComponents.JustText);
          text.append("<expansion>").append(doc.text).append("</expansion>\n");
        }
        return text.toString();
      default:
        throw new NotHandledNow("makeDocumentText", method);
    }
  }

  // Create an index and save it.
  public static void main(String[] args) throws Exception {
    Parameters argp = Parameters.parseArgs(args);
    AppFunction me = new BuildCategoryIndex();
    me.run(argp, System.out);
  }

  @Override
  public String getName() {
    return "build-category-index";
  }

  @Override
  public String getHelpString() {
    return makeHelpStr(
        "input", "flattened framework TSV that contains descriptions",
        "output", "output folder to write galago index",
        "method", "default=squash",
        "threaded", "default=false, whether to write output parts in parallel",
        "expansionIndex", "needed if method=expand."
        );
  }

  @Override
  public void run(Parameters argp, PrintStream stdout) throws Exception {
    String inputFramework = argp.getString("input");
    String outputFolder = argp.getString("output");
    boolean threadedWrite = argp.get("threaded", false);

    MemoryIndex memIndex = loadCategoriesInMemory(inputFramework, argp);

    FlushToDisk.flushMemoryIndex(memIndex, outputFolder, threadedWrite);
  }
}

