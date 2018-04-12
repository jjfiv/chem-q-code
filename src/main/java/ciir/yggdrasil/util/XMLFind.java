package ciir.yggdrasil.util;

import org.lemurproject.galago.utility.Parameters;
import org.lemurproject.galago.utility.json.JSONUtil;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @user jfoley
 */
public class XMLFind {
  public static Document loadAsDocumentPath(String path) {
    try {
      return DocumentBuilderFactory.newInstance()
          .newDocumentBuilder()
          .parse(new File(path));
    } catch (SAXException | ParserConfigurationException | IOException e) {
      throw new RuntimeException(e);
    }
  }


  public static String nodeAsString(Node n) {
    if(n == null) throw new RuntimeException("Node is null!");
    if(n.getNodeType() == Node.TEXT_NODE || n.getNodeType() == Node.CDATA_SECTION_NODE) {
      return n.getTextContent();
    } else if(n.getNodeType() == Node.ATTRIBUTE_NODE) {
      return n.getTextContent();
    }
    throw new RuntimeException("Not a terminal node! "+n);
  }

  public static String childNodesAsString(Node n) {
    NodeList children = n.getChildNodes();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < children.getLength(); i++) {
      sb.append(nodeAsString(children.item(i)));
    }
    return sb.toString();
  }

  public static List<Node> findChildrenByTagName(Node root, String tagName) {
    List<Node> output = new ArrayList<>();
    recursivelyFindChildrenByTagName(output, root, tagName);
    return output;
  }

  private static void recursivelyFindChildrenByTagName(List<Node> output, Node root, String query) {
    if(Objects.equals(root.getNodeName(), query)) {
      output.add(root);
      return;
    }
    NodeList children = root.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      recursivelyFindChildrenByTagName(output, children.item(i), query);
    }
  }

  public static Parameters getAttributes(Node xmlNode) {
    Parameters output = Parameters.create();
    NamedNodeMap attributes = xmlNode.getAttributes();
    for (int i = 0; i < attributes.getLength(); i++) {
      Node item = attributes.item(i);
      assert item.getNodeType() == Node.ATTRIBUTE_NODE : "Attribute map should have attribute nodes!";
      output.put(item.getNodeName(), JSONUtil.parseString(item.getTextContent()));
    }
    return output;
  }
}
