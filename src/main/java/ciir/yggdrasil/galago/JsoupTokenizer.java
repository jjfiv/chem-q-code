package ciir.yggdrasil.galago;

import ciir.jfoley.chai.string.StrUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;
import org.lemurproject.galago.core.parse.Document;
import org.lemurproject.galago.core.tokenize.Tokenizer;
import org.lemurproject.galago.tupleflow.FakeParameters;
import org.lemurproject.galago.tupleflow.TupleFlowParameters;
import org.lemurproject.galago.utility.Parameters;

import java.util.*;

/**
 * @user jfoley
 */
public class JsoupTokenizer extends Tokenizer {
  public static Tokenizer instance = new JsoupTokenizer();

  public JsoupTokenizer() {
    this(new FakeParameters(Parameters.create()));
  }
  public JsoupTokenizer(TupleFlowParameters parameters) {
    super(parameters);
  }

  // Stolen from TagTokenizer
  static Set<Character> splitChars = new HashSet<>(Arrays.asList(' ', '\t', '\n', '\r', // spaces
      ';', '\"', '&', '/', ':', '!', '#',
      '?', '$', '%', '(', ')', '@', '^',
      '*', '+', '-', ',', '=', '>', '<', '[',
      ']', '{', '}', '|', '`', '~', '_'));


  public void addToTermList(List<String> terms, String span) {
    StringBuilder fixed = new StringBuilder();
    for(char c : span.toCharArray()) {
      char ch = Character.toLowerCase(c);
      if(splitChars.contains(ch)) {
        fixed.append(' ');
        continue;
      }
      fixed.append(ch);
    }
    String txt = StrUtil.compactSpaces(fixed.toString());
    if(txt.isEmpty()) return;
    Collections.addAll(terms, txt.split("\\s+"));
  }


  @Override
  public void tokenize(final Document document) {
    org.jsoup.nodes.Document dom = Jsoup.parse(document.text);
    //dom.select("script").remove();
    document.terms = new ArrayList<>();

    dom.traverse(new NodeVisitor() {
      @Override
      public void head(Node node, int i) {
        if(node instanceof TextNode) {
          addToTermList(document.terms, ((TextNode) node).getWholeText());
        }
      }

      @Override
      public void tail(Node node, int i) {

      }
    });
  }
}
