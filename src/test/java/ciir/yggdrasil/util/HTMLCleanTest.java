package ciir.yggdrasil.util;

import org.junit.Assert;
import org.junit.Test;

public class HTMLCleanTest {
  @Test
  public void testEntityFixing() {
    Assert.assertEquals("<", HTMLClean.unescape("&lt;"));
    Assert.assertEquals(">", HTMLClean.unescape("&gt;"));
    Assert.assertEquals("\"", HTMLClean.unescape("&quot;"));
    Assert.assertEquals("\u03b2-D-glucose", HTMLClean.unescape("&beta;-D-glucose"));
    Assert.assertEquals("1<sup>1</sup> = 1", HTMLClean.unescape("1&lt;sup&gt;1&lt;/sup&gt; = 1"));
  }

  @Test
  public void testReplaceBRs() {
    Assert.assertEquals("hello\n\n\n", HTMLClean.replaceBRs("hello<br><BR><br />"));
    Assert.assertEquals("hello", HTMLClean.replaceBRs("hello<br><BR><br />").trim());
  }

}