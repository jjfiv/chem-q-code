package ciir.yggdrasil.chemistry.experiments;

import org.junit.Test;
import org.lemurproject.galago.core.eval.EvalDoc;
import org.lemurproject.galago.core.eval.SimpleEvalDoc;
import org.lemurproject.galago.utility.Parameters;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class RunExperimentTest {

  @Test
  public void testStandardRelevanceFeedback() {
    List<String> rels = Arrays.asList("rel1", "rel2");
    List<SimpleEvalDoc> docs = Arrays.asList(
        new SimpleEvalDoc("bad1", 1, 10),
        new SimpleEvalDoc("bad2", 2, 9),
        new SimpleEvalDoc("rel1", 3, 8),
        new SimpleEvalDoc("bad3", 4, 7),
        new SimpleEvalDoc("bad4", 5, 6),
        new SimpleEvalDoc("rel2", 6, 5),
        new SimpleEvalDoc("bad5", 7, 4),
        new SimpleEvalDoc("bad6", 8, 3)
    );

    List<? extends EvalDoc> newEvalDocs = RunExperiment.standardFeedback(Parameters.parseArray("srf-depth", 10), rels, docs);
    //for (EvalDoc evalDoc : newEvalDocs) {
      //System.out.printf("%s r:%d, s:%1.3f\n", evalDoc.getName(), evalDoc.getRank(), evalDoc.getScore());
    //}

    assertEquals("rel1", newEvalDocs.get(0).getName());
    assertEquals("rel2", newEvalDocs.get(1).getName());
    assertEquals("bad1", newEvalDocs.get(2).getName());

    List<? extends EvalDoc> halfHelpedDocs = RunExperiment.standardFeedback(Parameters.parseArray("srf-depth", 5), rels, docs);
    assertEquals("rel1", halfHelpedDocs.get(0).getName());
    assertEquals("bad1", halfHelpedDocs.get(1).getName());
    assertEquals("rel2", halfHelpedDocs.get(5).getName());
  }
}