package ciir.yggdrasil.chemistry;

import ciir.jfoley.chai.io.IO;
import ciir.jfoley.chai.string.StrUtil;
import ciir.yggdrasil.util.TSVLoader;
import org.lemurproject.galago.tupleflow.Utility;
import org.lemurproject.galago.utility.Parameters;
import org.lemurproject.galago.utility.json.JSONUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * @author jfoley
 */
public class HierarchyCleaning {
  Map<Integer, Node> nodes;
  HierarchyCleaning(Map<Integer, Node> loadedNodes) {
    nodes = loadedNodes;
  }

  public Map<String,Node> byTitle() {
    HashMap<String,Node> results = new HashMap<>();
    for (Node node : nodes.values()) {
      results.put(node.getNodeAddress(), node);
    }
    return results;
  }

  public static class Node {
    public int umassId;
    public int acsId;
    public String usefulId;

    public final int level; // one of 1,2,3,4
    public String title;
    public String description;

    public Set<Node> children;
    public Node parent;

    public String getNodeAddress() {
      String address = title;
      if(level == 1) {
        // Drop name from title for first level.
        address = StrUtil.takeBefore(title, ". ")+".";
      }
      return address;
    }

    @Override
    public int hashCode() {
      return getNodeAddress().hashCode();
    }

    @Override
    public boolean equals(Object other) {
      if(other instanceof Node) {
        Node n = (Node) other;
        return Objects.equals(title, n.title);
      }
      return false;
    }

    public Node(int selfId, int level, String title, String description) {
      this.umassId = selfId;
      this.level = level;
      this.title = title;
      this.description = description;
      this.children = new HashSet<>();
      this.parent = null;
      this.acsId = -1;
    }

    public Parameters toJSON() {
      Parameters out = Parameters.create();
      if(umassId != -1) {
        out.put("umassId", umassId);
      }
      out.put("id", usefulId);
      if(acsId != -1) {
        out.put("acsId", acsId);
      }
      out.put("level", level);
      out.put("title", title);
      out.put("description", description);
      out.put("address", getNodeAddress());
      if(parent != null) {
        out.put("parent", parent.usefulId);
      }
      List<String> children = new ArrayList<>();
      for (Node child : this.children) {
        children.add(child.usefulId);
      }
      out.put("children", children);

      return out;
    }

    public int getId() {
      return umassId;
    }
  }

  public static List<String> generateParentsIncludingSelfByAddress(String address) {
    List<String> parts = Arrays.asList(address.split("\\."));
    assert(parts.size() <= 4);

    List<String> heads = new ArrayList<>();
    for(int i=1; i<=parts.size(); i++) {
      heads.add(Utility.join(parts.subList(0, i), ".")+".");
    }

    return heads;
  }

  public static String FixTitle(String input) {
    int start = input.indexOf(".g.");
    if(start >= 0) {
      return input.substring(0, start) + input.substring(start+2);
    }
    return input;
  }

  public static HierarchyCleaning loadUMassVersion(String path) {
    List<Parameters> rows = TSVLoader.withHeader(path);
    Map<Integer,Node> dataset = new HashMap<>();
    for (Parameters row : rows) {
      int level4 = (int) row.getLong("FrameworkCategoryID4");
      int level3 = (int) row.getLong("FrameworkCategoryID3");
      int level2 = (int) row.getLong("FrameworkCategoryID2");
      int level1 = (int) row.getLong("FrameworkCategoryID1");

      String title4 = FixTitle(row.getString("Level4Title"));
      String title3 = FixTitle(row.getString("Level3Title"));
      String title2 = FixTitle(row.getString("Level2Title"));
      String title1 = FixTitle(row.getString("Level1Title"));

      if(title4.contains(".o.")) { continue; } // skip any Organic Chemistry.
      assert(row.getString("Level4Title").contains(".g."));

      // create any nodes we need:
      dataset.put(level4, new Node(level4, 4, title4, row.getString("Level4Description")));
      if(!dataset.containsKey(level3)) {
        dataset.put(level3, new Node(level3, 3, title3, row.getString("Level3Description")));
      }
      if(!dataset.containsKey(level2)) {
        dataset.put(level2, new Node(level2, 2, title2, row.getString("Level2Description")));
      }
      if(!dataset.containsKey(level1)) {
        dataset.put(level1, new Node(level1, 1, title1, row.getString("Level1Description")));
      }

      // connect any nodes we can:
      Node n1 = dataset.get(level1);
      Node n2 = dataset.get(level2);
      Node n3 = dataset.get(level3);
      Node n4 = dataset.get(level4);
      setParentChild(n1, n2);
      setParentChild(n2, n3);
      setParentChild(n3, n4);
    }

    return new HierarchyCleaning(dataset);
  }
  static void setParentChild(Node p, Node c) {
    p.children.add(c);
    c.parent = p;
  }


  // This is just to see if we can load a hierarchy correctly.
  public void writeGraphvizGraph(PrintWriter out) {
    out.println("// Automatically generated.");
    out.println("graph hierarchy {");

    List<Node> topLevel = new ArrayList<>();
    for (Node node : nodes.values()) {
      out.printf("n%d [label=\"%s\"];\n", node.getId(), JSONUtil.escape(node.title)); //+": "+node.description));
      if(node.parent != null) {
        out.printf("n%d -- n%d;\n", node.getId(), node.parent.getId());
      }
      if(node.level == 1) {
        topLevel.add(node);
      }
    }

    out.println("root;\n");
    for (Node node : topLevel) {
      out.printf("n%d -- root;\n", node.getId());
    }
    out.println("} // end graph hierarchy");
  }

  public static String makeTitle(String p1, String p2, String p3, String p4) {
    // Capital Roman // Capital Alpha // Lowercase Alpha // Numeric
    if (p2.isEmpty()) {
      return String.format("%s.", p1);
    } else if (p3.isEmpty()) {
      return String.format("%s.%s.", p1, p2);
    } else if (p4.isEmpty()) {
      return String.format("%s.%s.%s.", p1, p2, p3);
    } else {
      return String.format("%s.%s.%s.%s.", p1, p2, p3, p4);
    }
  }

  public static void main(String[] args) throws IOException {
    Parameters opts = Parameters.parseArgs(args);
    HierarchyCleaning h = HierarchyCleaning.loadUMassVersion("chemistry/flat-framework.tsv");

    List<Parameters> acsHierarchy = TSVLoader.withHeader("chemistry/acs-hierarchy.tsv");
    Map<String, Node> titleMapping = h.byTitle();

    for (Parameters entry : acsHierarchy) {
      String p1 = entry.getAsString("ACCM Big Idea").trim();
      String p2 = entry.getAsString("ACCM Enduring Understanding").trim();
      String p3 = entry.getAsString("ACCM Sub-Disciplinary Articulation (L3)").trim();
      String p4 = entry.getAsString("ACCM Content Details (L4)").trim();
      int acsId = (int) entry.getLong("ACCM L1L2L3L4");
      String description = entry.getString("Statement").replace("\u2013", "-");

      int level = 4;
      if(p2.isEmpty())  {
        level = 1;
      } else if(p3.isEmpty()) {
        level = 2;
      } else if(p4.isEmpty()) {
        level = 3;
      }
      String title = makeTitle(p1, p2, p3, p4);

      Node mappedToUMassHierarchy = titleMapping.get(title);
      if(mappedToUMassHierarchy == null) {
        continue;
      }
      mappedToUMassHierarchy.acsId = acsId;
    }

    // Validate hierarchy:
    List<Parameters> data = new ArrayList<>();
    String name = "drop-jan-16";
    int index = 0;
    for(Node n : titleMapping.values()) {
      n.usefulId = String.format("%s.%d", name, index++);
      if (n.parent != null) {
        assert (n.parent.children.contains(n));
      }
    }
    // Now the node ids are not null.
    for(Node n : titleMapping.values()) {
      data.add(n.toJSON());
    }

    Parameters hierarchyJSON = Parameters.parseArray("data", data);
    try(PrintWriter out = IO.openPrintWriter("hierarchy.json")) {
      out.println(hierarchyJSON.toPrettyString());
    }

    //System.out.println(RandUtil.sampleRandomly(h.nodes.values(), 1, new Random()).get(0).title);
    try(PrintWriter out = IO.openPrintWriter("hierarchy.dot")) {
      h.writeGraphvizGraph(out);
    }
  }
}
