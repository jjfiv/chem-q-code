package ciir.yggdrasil.chemistry.experiments;

import org.junit.Test;
import org.lemurproject.galago.utility.Parameters;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class QuestionContextModelTest {
  @Test
  public void lookForQuestionMark() throws IOException {
    String originalQuestion = "This is the first sentence of setup. This is the second sentence. What should I remember?\n" +
      "A - Something here\nB - Something else\nC - Third thing\nD - Correct answer.";

    List<String> originalTokens = QuestionContextModelParser.sentenceSplitter(originalQuestion);
    assertEquals("This is the first sentence of setup", originalTokens.get(0));
    assertEquals(".", originalTokens.get(1));
    assertEquals("B - Something else", originalTokens.get(8));

    String questionStatement = QuestionContextModelParser.extractQuestionPieces(originalQuestion).get("question-statement");
    assertEquals("What should I remember?", questionStatement);
  }

  @Test
  public void breadthTest() throws IOException {
    // Make sure we don't crash on any inputs.
    try (ExperimentResources resources = new ExperimentResources(ExperimentCommon.DefaultParameters)) {
      for (Parameters question : resources.getAllQuestions()) {
        Map<String,String> qparts = QuestionContextModelParser.extractQuestionPieces(question.getString("text"));
        assertFalse(qparts.isEmpty());
      }
    }

  }

}