package ciir.yggdrasil.chemistry;

import ciir.yggdrasil.galago.JsoupTokenizer;
import ciir.yggdrasil.util.XMLFind;
import org.jsoup.Jsoup;
import org.lemurproject.galago.utility.Parameters;
import org.lemurproject.galago.utility.json.JSONUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author jfoley
 */
public class OWLGeneratedQuestionLoader {
  public static List<Parameters> load() throws IOException {
    String path = "chemistry/owl-acsexamsinstitutetest-owl-questions.xml";
    Map<Integer, List<Integer>> judgedQids = JudgmentLoader.load("chemistry/level3questionlinks.tsv", "chemistry/level4questionlinks.tsv");
    List<Parameters> collectedQuestions = new ArrayList<>();

    Document document = XMLFind.loadAsDocumentPath(path);
    List<Node> questions = XMLFind.findChildrenByTagName(document, "question");

    for(Node question : questions) {
      Parameters p = parseQuestion(question);
      int id = (int) p.getLong("id");
      if(judgedQids.containsKey(id)) {
        p.set("judgment", judgedQids.get(id));
      }
      collectedQuestions.add(p);
    }

    return collectedQuestions;
  }

  private static Parameters parseQuestion(Node item) {
    Parameters output = Parameters.create();

    output.put("iu",
        JSONUtil.parseString(XMLFind.nodeAsString(item.getAttributes().getNamedItem("iu"))));
    output.put("id",
        JSONUtil.parseString(XMLFind.nodeAsString(item.getAttributes().getNamedItem("id"))));
    output.put("srand",
        JSONUtil.parseString(XMLFind.nodeAsString(item.getAttributes().getNamedItem("srand"))));

    int id = (int) output.getLong("id");

    List<Node> askNodes = XMLFind.findChildrenByTagName(item, "ask");
    assert(askNodes.size() == 1);

    String html = XMLFind.childNodesAsString(askNodes.get(0));
    org.jsoup.nodes.Document dom = Jsoup.parse(html);
    dom.select("script").remove();
    output.put("html", dom.toString());
    output.put("terms", JsoupTokenizer.instance.tokenize(html).terms);
    return output;
  }

}
