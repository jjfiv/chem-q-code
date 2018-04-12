package ciir.yggdrasil.galago;

import org.junit.Test;
import org.lemurproject.galago.core.parse.Document;

import static org.junit.Assert.*;

public class JsoupTokenizerTest {

  @Test
  public void testSkipsScripts() {
    String test_data = "<script> function() { return \"foo\"; }</script>hello World!";
    Document doc = JsoupTokenizer.instance.tokenize(test_data);
    assertEquals(2, doc.terms.size());
    assertEquals("hello", doc.terms.get(0));
    assertEquals("world", doc.terms.get(1));

  }

  @Test
  public void testNesting() {
    String input_data =
      "      <td valign=\"TOP\">O<sub>2</sub> dissolved in water <i>and</i> O removed from H<sub>2</sub>O<br /><br /></td>\n";

    Document doc = JsoupTokenizer.instance.tokenize(input_data);
    assertArrayEquals("o 2 dissolved in water and o removed from h 2 o".split(" "), doc.terms.toArray());
  }

}