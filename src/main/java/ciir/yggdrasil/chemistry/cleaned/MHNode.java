package ciir.yggdrasil.chemistry.cleaned;

import ciir.jfoley.chai.string.StrUtil;
import org.lemurproject.galago.utility.Parameters;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
* @author jfoley.
*/
public class MHNode {
  public String id;
  Integer umassId;
  Integer acsId;
  public int level;
  public String title;
  public String description;

  /** which of the root nodes there is. */
  public MHNode root;
  public List<MHNode> children;

  final Parameters json;
  public MHNode parent;

  public MHNode(Parameters json) {
    this.json = json;
    this.id = json.getString("id");
    if(json.isLong("umassId")) {
      this.umassId = (int) json.getLong("umassId");
    }
    if(json.isLong("acsId")) {
      this.acsId = (int) json.getLong("acsId");
    }
    assert(acsId != null || umassId != null);

    this.level = (int) json.getLong("level");
    this.title = json.getString("title");
    this.description = json.getString("description");
    this.children = new ArrayList<>();
  }

  public void init(Map<String, MHNode> data) {
    for (String childId : json.getAsList("children", String.class)) {
      children.add(data.get(childId));
    }
    if(json.containsKey("parent")) {
      parent = data.get(json.getString("parent"));
    }
  }

  public String getRootRomanName() {
    return StrUtil.takeBefore(title, ".");
  }

  public String getRootId() {
    return root.id;
  }

  public List<MHNode> descendants() {
    List<MHNode> output = new ArrayList<>();
    LinkedList<MHNode> frontier = new LinkedList<>();
    frontier.add(this);
    while(true) {
      if(frontier.isEmpty()) break;

      MHNode ptr = frontier.pop();
      output.add(ptr);
      for (MHNode child : ptr.children) {
        frontier.add(child);
      }
    }

    return output;
  }

  @Override
  public String toString() {
    return this.title +" : "+ this.id;
  }

  public List<String> getNPath() {
    List<String> myDirectParentsAndMe = new ArrayList<>();
    for(MHNode ptr = this; ptr != null; ptr = ptr.parent) {
      //System.out.println("getNPath: "+this+" .. "+ptr);
      myDirectParentsAndMe.add(ptr.id);
    }
    return myDirectParentsAndMe;
  }
}
