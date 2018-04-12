package ciir.yggdrasil.stackexchange;

import ciir.yggdrasil.util.XMLFind;
import org.lemurproject.galago.utility.Parameters;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jfoley
 */
public class PostParser {
  public static final String PostsFile = "Posts.xml";
  public static final String CommentsFile = "Comments.xml";

  public static List<Parameters> loadRowsFromFile(String path) {
    Document xml = XMLFind.loadAsDocumentPath(path);
    List<Node> posts = XMLFind.findChildrenByTagName(xml, "row");
    List<Parameters> output = new ArrayList<>(posts.size());

    for (Node post : posts) {
      output.add(XMLFind.getAttributes(post));
    }

    return output;
  }

  public static void main(String[] args) throws IOException {
    String basePath = "/home/jfoley/Downloads/chemistry.stackexchange.com/";
    String path = "Comments.xml";

    List<Parameters> posts = loadRowsFromFile(basePath + PostsFile);
    List<Parameters> comments = loadRowsFromFile(basePath + CommentsFile);
    System.out.println("Total Posts: "+posts.size());
    System.out.println("First Post: "+posts.get(0).toPrettyString());

    for (Parameters post : posts) {
      if(post.get("ParentId", -1) == posts.get(0).getLong("Id")) {
        System.out.println("Answer for #1: "+post);
      }
    }

    System.out.println("Total Comments: "+comments.size());
    System.out.println("First Comment: " + comments.get(0).toPrettyString());

  }
}
