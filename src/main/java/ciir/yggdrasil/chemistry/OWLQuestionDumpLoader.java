package ciir.yggdrasil.chemistry;

import ciir.jfoley.chai.string.StrUtil;
import ciir.yggdrasil.util.HTMLClean;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/* Structure of file:
 * <owlcontent owlversion="24.7">
 * ..
 * header data
 * ..
 * <body>
 * <IU>
 *   <QuestionTable>...</QuestionTable>
 *   <QuestionTable>...</QuestionTable>
 *   ...
 * </IU>
 * <tags>
 *   <UserTags>...</UserTags>
 *   <UserTags>...</UserTags>
 *   ...
 * </tags>
 * <usertables />
 * </body>
 *
 */

/**
 *
 *
 * @author jfoley.
 */
public class OWLQuestionDumpLoader {

  public static class Question {
    int questionId;
    int iuNumber;
    String description;
    String ask;
    int type;
    int sequence;
    List<Answer> choices;
    public Question(int questionId, int iuNumber, String description, int type, int sequence, String ask, List<Answer> choices) {
      this.questionId = questionId;
      this.iuNumber = iuNumber;
      this.description = description;
      this.type = type;
      this.sequence = sequence;
      this.ask = ask;
      this.choices = choices;
    }
  }

  public static class Answer {
    public int index;
    public boolean enabled;
    public String text;
    public String value;

    public Answer(int index, boolean enabled, String text, String value) {
      this.index = index;
      this.enabled = enabled;
      this.text = text;
      this.value = value;
    }
  }

  public static void main(String[] args) throws IOException, XMLStreamException {
    String path = "chemistry/OWL-Question-dump.xml";

    Map<Integer, List<Integer>> judgedQids = JudgmentLoader.load("chemistry/level3questionlinks.tsv", "chemistry/level4questionlinks.tsv");

    Document parse = Jsoup.parse(new File(path), "ISO-8859-1");

    // Parse Questions.
    List<Question> questions = parseQuestions(parse.select("QuestionTable"));
    System.out.println("Found " + questions.size() + " questions!");

    Elements userTags = parse.select("UserTags");
    System.out.println(userTags.get(0));

    /*
    ALL GIFS in current dump.
    for (Element userTag : userTags) {
      System.out.println(HTMLClean.unescape(userTag.select("replacementtext").html()));
    }
    */

    System.out.println("NUM_JUDGED: "+judgedQids.size());
    int totalJudgedThatWeHave = 0;
    for (Question question : questions) {
      System.out.println(question.questionId);
      if(judgedQids.containsKey(question.questionId)) {
        totalJudgedThatWeHave++;
      }
    }
    System.out.println("NUM_JUDGED THAT WE HAVE: "+totalJudgedThatWeHave);


    //
    //printQuestionList(questions);
  }

  public static List<Question> parseQuestions(Elements questionTags) {
    List<Question> questions = new ArrayList<>();
    for (Element q : questionTags) {
      String askEscaped = q.select("ask").text();
      String ask = HTMLClean.unescape(askEscaped);

      int questionId = Integer.parseInt(q.select("questionid").html());
      int iuNumber = Integer.parseInt(q.select("iunumber").html());
      String description = q.select("description").html();
      int type = Integer.parseInt(q.select("type").html());
      int sequence = Integer.parseInt(q.select("sequence").html());
      Elements answerTags = q.select("answer");

      List<Answer> choices = new ArrayList<>();
      for (Element answer : answerTags) {
        if(answer.hasAttr("dbt")) continue;
        int index = Integer.parseInt(answer.attr("index"));
        boolean enabled = Boolean.parseBoolean(answer.attr("enabled"));
        String text =
          cleanAnswerText(answer.select("text").html());
        String value = answer.select("value").html();
        choices.add(new Answer(index, enabled, text, value));
      }

      questions.add(new Question(questionId, iuNumber, description, type, sequence, ask, choices));
    }
    return questions;
  }

  private static void printQuestionList(List<Question> questions) {
    for(Question q : questions) {
      System.out.println(q.ask);
      for(Answer a : q.choices) {
        System.out.println("\t"+a.text);
      }
    }
  }

  private static String cleanAnswerText(String text) {
    // needs two-passes of unescape: &amp;ndash; -> &ndash; -> -
    return StrUtil.compactSpaces(
        HTMLClean.replaceBRs(
            HTMLClean.unescape(
                HTMLClean.unescape(text))));
  }
}
