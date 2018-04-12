package ciir.yggdrasil.chemistry.cleaned;

import ciir.jfoley.chai.string.StrUtil;
import org.lemurproject.galago.utility.Parameters;

import java.io.IOException;
import java.util.*;

/**
 * @author jfoley
 */
public class MergedHierarchy {
  public Map<String, MHNode> nodesById;
  Map<Integer, MHNode> nodesByACCM;
  Map<Integer, MHNode> nodesByUMass;

  Map<String, MHNode> rootsByRoman;

  MergedHierarchy(Map<String,MHNode> data) {
    nodesById = data;
    nodesByACCM = new HashMap<>();
    nodesByUMass = new HashMap<>();
    rootsByRoman = new HashMap<>();
    for (MHNode node : nodesById.values()) {
      if(node.umassId != null) {
        nodesByUMass.put(node.umassId, node);
      }
      if(node.acsId != null) {
        nodesByACCM.put(node.acsId, node);
      }
      if(node.level == 1) {
        String rootRoman = StrUtil.takeBefore(node.title, ".");
        rootsByRoman.put(rootRoman, node);
      }
    }

    for (MHNode mhNode : nodesById.values()) {
      mhNode.root = rootsByRoman.get(mhNode.getRootRomanName());
    }

  }

  public Collection<MHNode> getNodes() {
    return nodesById.values();
  }

  public MHNode lookupACCM(int id) {
    return nodesByACCM.get(id);
  }
  public MHNode lookupUMass(int id) {
    return nodesByUMass.get(id);
  }
  public MHNode lookup(String id) { return nodesById.get(id); }

  public static MergedHierarchy load(String path) {
    HashMap<String,MHNode> data = new HashMap<>();

    List<Parameters> nodePs;
    try {
      nodePs = Parameters.parseFile(path).getAsList("data", Parameters.class);
    } catch (IOException e) {
      throw new RuntimeException(path+" is not parsable as Parameters", e);
    }

    for (Parameters nodeP : nodePs) {
      MHNode n = new MHNode(nodeP);
      data.put(n.id, n);
    }

    for (MHNode node : data.values()) {
      node.init(data);
    }

    return new MergedHierarchy(data);
  }

  public static void main(String[] args) {
    MergedHierarchy mh = load("hierarchies/hierarchy.fixed-jan-23.json");
    System.out.println(mh.nodesById.size());
  }

}
