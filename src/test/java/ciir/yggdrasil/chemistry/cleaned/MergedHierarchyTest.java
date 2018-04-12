package ciir.yggdrasil.chemistry.cleaned;

import ciir.yggdrasil.chemistry.experiments.ExperimentCommon;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MergedHierarchyTest {
  @Test
  public void testDescendants() {
    MergedHierarchy h = MergedHierarchy.load(ExperimentCommon.DefaultParameters.getString("hierarchy"));
    for (MHNode mhNode : h.getNodes()) {
      //System.out.println("Level: "+mhNode.level+" Descendants: "+mhNode.descendants().size());

      if(mhNode.level == 4) {
        assertEquals(1, mhNode.descendants().size());
        assertEquals(mhNode.parent.children.size()+1, mhNode.parent.descendants().size());
      } else if(mhNode.level == 3) {
        assertEquals(mhNode.children.size() + 1, mhNode.descendants().size());
      }
      if(mhNode.level > 1) {
        assertNotNull(mhNode.parent);
        assertTrue(mhNode.parent.descendants().size() > mhNode.descendants().size());
      }
    }
  }

  @Test
  public void testNPath() {
    MergedHierarchy h = MergedHierarchy.load(ExperimentCommon.DefaultParameters.getString("hierarchy"));
    for (MHNode mhNode : h.getNodes()) {
      if(mhNode.level == 4) {
        assertEquals(3, mhNode.parent.level);
        assertEquals(4, mhNode.getNPath().size());
      } else if(mhNode.level == 3) {
        assertEquals(2, mhNode.parent.level);
        assertEquals(3, mhNode.getNPath().size());
      }
    }
  }
}