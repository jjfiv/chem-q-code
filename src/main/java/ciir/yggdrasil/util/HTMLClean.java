package ciir.yggdrasil.util;

import java.util.HashMap;
import java.util.Map;

/**
 * @author jfoley.
 */
public class HTMLClean {

  public static Map<String,String> EntityTable = new HashMap<>();
  static {
    EntityTable.put("gt", ">");
    EntityTable.put("lt", "<");
    EntityTable.put("quot", "\"");
    EntityTable.put("beta", "\u03b2");
    EntityTable.put("ndash", "-");
    EntityTable.put("amp", "&");

    System.out.println(EntityTable);
  }

  public static String unescape(String input) {
    StringBuilder output = new StringBuilder();
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if(c != '&') {
        output.append(c);
        continue;
      }

      boolean replaced = false;
      for(String key : EntityTable.keySet()) {
        boolean found = true;
        for (int j = 0; j < key.length(); j++) {
          if(key.charAt(j) != input.charAt(i+j+1)) {
            found = false;
            break;
          }
        }
        if(!found) continue;
        if(input.charAt(i+key.length()+1) == ';') {
          i += key.length()+1;
          output.append(EntityTable.get(key));
          replaced = true;
          break;
        }
      }

      if(replaced) continue;
      throw new IllegalStateException("Couldn't handle escape "+input.substring(i, Math.min(i+20, input.length())));
    }
    return output.toString();
  }

  public static String replaceBRs(String input) {
    return input.replaceAll("<(b|B)(r|R)\\s?/?>", "\n");
  }

}
