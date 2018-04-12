package ciir.yggdrasil.chemistry;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class HierarchyTest {
  @Test
  public void testCleansWell() {
    assertEquals("I.A.1.", HierarchyCleaning.FixTitle("I.A.g.1."));
    assertEquals("I.G.1.", HierarchyCleaning.FixTitle("I.G.g.1."));
  }

  @Test
  public void testGeneratesParents() {
    assertEquals(Arrays.asList("I.", "I.A.", "I.A.1."), HierarchyCleaning.generateParentsIncludingSelfByAddress("I.A.1."));
  }

}