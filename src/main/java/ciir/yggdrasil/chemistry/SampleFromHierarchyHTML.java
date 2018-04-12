package ciir.yggdrasil.chemistry;

import ciir.jfoley.chai.collections.util.IterableFns;
import ciir.jfoley.chai.collections.util.MapFns;
import ciir.jfoley.chai.io.IO;
import ciir.yggdrasil.chemistry.cleaned.MHNode;
import ciir.yggdrasil.chemistry.cleaned.MergedHierarchy;
import ciir.yggdrasil.chemistry.experiments.ExperimentCommon;
import ciir.yggdrasil.chemistry.experiments.ExperimentResources;
import org.lemurproject.galago.utility.Parameters;
import org.lemurproject.galago.utility.tools.AppFunction;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jfoley
 */
public class SampleFromHierarchyHTML extends AppFunction {


  @Override
  public String getName() {
    return "sample-from-hierarchy-html";
  }

  @Override
  public String getHelpString() {
    return makeHelpStr(
        "something", "Todo"
    );
  }

  @Override
  public void run(Parameters p, PrintStream printStream) throws Exception {
    Parameters argp = ExperimentCommon.DefaultParameters.clone();
    argp.copyFrom(p);

    Map<String, List<Parameters>> questionsAlignedToTopLevel = new HashMap<>();
    try (ExperimentResources resources = new ExperimentResources(argp);
         PrintWriter output = IO.openPrintWriter(argp.getString("output"))) {
      MergedHierarchy hierarchy = resources.getHierarchy();
      for (Parameters question : resources.getAllQuestions()) {
        for (String nodeId : question.getAsList("judgments", String.class)) {
          String root = hierarchy.lookup(nodeId).getRootId();
          MapFns.extendListInMap(questionsAlignedToTopLevel, root, question);
        }
      }
      output.println("<html>");
      output.println("<head>");
      output.println("</head>");
      output.println("<body>");
      for (String topId : IterableFns.sorted(questionsAlignedToTopLevel.keySet())) {
        List<Parameters> questions = questionsAlignedToTopLevel.get(topId);
        MHNode root = hierarchy.lookup(topId);
        output.println("<div>");
        output.printf("<h3>%s: %s</h3>\n", root.title, root.description);
        output.println("<table>");
        for (int i = 0; i < Math.min(5, questions.size()); i++) {
          output.printf("<tr><td>%s</td></tr>", questions.get(i).getString("text"));
        }
        output.println("</table>");
      }
      output.println("</body>");
      output.println("</html>");
    }
  }


}
