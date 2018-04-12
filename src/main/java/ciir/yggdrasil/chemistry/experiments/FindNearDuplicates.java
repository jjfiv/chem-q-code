package ciir.yggdrasil.chemistry.experiments;

import ciir.jfoley.chai.collections.util.SetFns;
import org.lemurproject.galago.utility.FixedSizeMinHeap;
import org.lemurproject.galago.utility.Parameters;
import org.lemurproject.galago.utility.lists.Scored;
import org.lemurproject.galago.utility.tools.AppFunction;
import org.lemurproject.galago.utility.CmpUtil;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author jfoley.
 */
public class FindNearDuplicates extends AppFunction {

  @Override
  public String getName() {
    return "find-near-duplicates";
  }

  @Override
  public String getHelpString() {
    return makeHelpStr("defaults...", "see the other thing");
  }

  public static class PossibleDuplicate extends Scored {

    public final Parameters left;
    public final Parameters right;

    public PossibleDuplicate(Parameters pLeft, Parameters pRight, double score) {
      super(score);
      this.left = pLeft;
      this.right = pRight;
    }

    @Override
    public Scored clone(double score) {
      return new PossibleDuplicate(this.left, this.right, score);
    }
  }

  @Override
  public void run(Parameters p, PrintStream stdout) throws Exception {
    Parameters argp = ExperimentCommon.DefaultParameters.clone();
    argp.copyFrom(p);

    Comparator<PossibleDuplicate> cmp = new Comparator<PossibleDuplicate>() {
      @Override
      public int compare(PossibleDuplicate lhs, PossibleDuplicate rhs) {
        return CmpUtil.compare(lhs.score, rhs.score);
      }
    };

    FixedSizeMinHeap<PossibleDuplicate> mostLikelyDuplicates = new FixedSizeMinHeap<PossibleDuplicate>(
        PossibleDuplicate.class, (int) argp.get("requested", 100), cmp);

    try (ExperimentResources resources = new ExperimentResources(argp)) {
      List<Parameters> allQuestions = resources.getAllQuestions();

      for (int i = 0; i < allQuestions.size(); i++) {
        Parameters qx = allQuestions.get(i);
        List<String> xTerms = resources.tokenizer.tokenize(qx.getString("text")).terms;
        for (int j = i + 1; j < allQuestions.size(); j++) {
          Parameters qy = allQuestions.get(j);
          // don't compare similarity between the same things.
          List<String> yTerms = resources.tokenizer.tokenize(qy.getString("text")).terms;

          double score = similarity(argp, xTerms, yTerms);
          mostLikelyDuplicates.offer(new PossibleDuplicate(qx, qy, score));
        }
      }

      stdout.println("<table border=1 cellspacing=0>");
      stdout.println("<tr><td>X</td><td>Y</td><td>isect</td><td>union</td><td>Score</td></tr>");
      for (Scored scored : mostLikelyDuplicates.getSortedArray()) {
        System.err.println(scored.score);
        PossibleDuplicate dup = (PossibleDuplicate) scored;
        List<String> lTerms = resources.tokenizer.tokenize(dup.left.getString("text")).terms;
        List<String> rTerms = resources.tokenizer.tokenize(dup.right.getString("text")).terms;
        stdout.printf(
            "<tr><td><b>%s</b><br />%s</td><td><b>%s</b><br />%s</td><td>%s</td><td>%s</td><td>XJ: %s, YJ: %s, %1.3f</td></tr>\n",
            dup.left.getString("qid"), dup.left.getString("text"), dup.right.getString("qid"),
            dup.right.getString("text"), SetFns.intersection(new HashSet<>(lTerms), new HashSet<>(rTerms)),
            SetFns.union(new HashSet<>(lTerms), new HashSet<>(rTerms)), dup.left.getAsList("judgments", String.class),
            dup.right.getAsList("judgments", String.class), dup.score);

      }
      stdout.println("</table>");
    }

  }

  private double similarity(Parameters argp, List<String> xTerms, List<String> yTerms) {
    String method = argp.getString("similarity");
    switch (method) {
    case "isect-size": {
      Set<String> xs = new HashSet<>(xTerms);
      Set<String> ys = new HashSet<>(yTerms);
      return ((double) SetFns.intersection(xs, ys).size()) / ((double) SetFns.union(xs, ys).size());
    }
    default:
      break;
    }
    throw new RuntimeException("No such similarity method " + method);
  }
}
