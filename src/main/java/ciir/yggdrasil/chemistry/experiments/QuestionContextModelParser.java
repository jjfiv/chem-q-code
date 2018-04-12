package ciir.yggdrasil.chemistry.experiments;

import ciir.jfoley.chai.string.StrUtil;
import ciir.yggdrasil.util.SGML;
import org.lemurproject.galago.utility.CmpUtil;

import java.util.*;

/**
 * @author jfoley.
 */
public class QuestionContextModelParser {
  public static List<String> sentenceSplitter(String input) {
    input = SGML.removeTagsLeaveContents(input).trim();
    Set<Character> punctuation = new HashSet<>(Arrays.asList('.', ',', '\n', '?', '!'));

    boolean inParens = false;
    List<Integer> punct = new ArrayList<>();
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if(c == '(') {
        inParens = true;
        continue;
      }
      if(inParens && c == ')') {
        inParens = false;
        continue;
      }
      if(inParens) continue;
      if(c == '.' && Character.isDigit(input.charAt(i-1))) {
        continue;
      }
      if(punctuation.contains(c)) {
        punct.add(i);
      }
    }

    List<String> parts = new ArrayList<>();
    int last = 0;
    for (int idx : punct) {
      String sentence = StrUtil.compactSpaces(input.substring(last, idx));
      String ps = input.substring(idx, idx + 1);

      if(Objects.equals(ps, "\n") && sentence.isEmpty()) {
        last = idx+1;
        continue;
      }
      parts.add(sentence);
      //System.out.println(Parameters.parseArray("last", last, "idx", idx, "sentence", sentence, "ps", ps));
      parts.add(ps);
      last = idx+1;
    }
    parts.add(input.substring(last));
    return parts;
  }

  public static Map<String, String> extractQuestionPieces(String text) {
    List<String> tokens = sentenceSplitter(text);
    List<Integer> questionMarks = new ArrayList<>();
    for (int i = 0; i < tokens.size(); i++) {
      String token = tokens.get(i);
      if(token.equals("?")) {
        questionMarks.add(i);
      }
    }

    String questionStatement;

    if(questionMarks.size() == 1) {
      int mark = questionMarks.get(0);
      questionStatement = tokens.get(mark - 1) + tokens.get(mark);
      // Important sentence starts with Identify and ends with a period... fifth back.
      // Single sentence with
    } else if(tokens.size() < 2) {
      questionStatement = tokens.get(0);
    } else if(tokens.size() <= 10) {
      // one statement, four answers + token sep
      questionStatement = tokens.get(0) + tokens.get(1);
    } else if(tokens.size() <= 14) {
      // one statement, four answers + token sep
      questionStatement = tokens.get(0) + tokens.get(1) + tokens.get(2);
    } else {
      //System.out.println(tokens);
      //return Collections.emptyMap();
      //throw new RuntimeException(text);
      questionStatement = Collections.max(tokens, new Comparator<String>() {
        @Override
        public int compare(String lhs, String rhs) {
          return CmpUtil.compare(lhs.length(), rhs.length());
        }
      });
    }

    Map<String,String> output = new HashMap<>();
    output.put("full", text);
    output.put("question-statement", questionStatement);

    return output;
  }
}
