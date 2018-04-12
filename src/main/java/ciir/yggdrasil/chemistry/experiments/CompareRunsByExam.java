package ciir.yggdrasil.chemistry.experiments;

import ciir.jfoley.chai.collections.util.IterableFns;
import gnu.trove.list.array.TDoubleArrayList;
import org.lemurproject.galago.utility.Parameters;
import org.lemurproject.galago.utility.tools.AppFunction;

import java.io.PrintStream;

/**
 * @author jfoley.
 */
public class CompareRunsByExam extends AppFunction {

  @Override
  public String getName() {
    return "compare-runs-by-exam";
  }

  @Override
  public String getHelpString() {
    return makeHelpStr("a", "a.json", "b", "b.json");
  }

  @Override
  public void run(Parameters p, PrintStream stdout) throws Exception {
    String metric = p.getString("metric");
    Parameters a = Parameters.parseFile(p.getString("a"));
    Parameters b = Parameters.parseFile(p.getString("b"));

    Parameters examsA = a.getMap("byExam");
    Parameters examsB = b.getMap("byExam");

    TDoubleArrayList valsA = new TDoubleArrayList();
    TDoubleArrayList valsB = new TDoubleArrayList();

    stdout.println("Exam ("+metric+")\t"+p.getString("a")+"\t"+p.getString("b"));
    for (String exam : IterableFns.sorted(examsA.keySet())) {
      double valA = examsA.getMap(exam).getDouble(metric);
      double valB = examsB.getMap(exam).getDouble(metric);

      stdout.println(exam + "\t" + valA + "\t" + valB);
      valsA.add(valA);
      valsB.add(valB);
    }

    stdout.println("Mean\t"+valsA.sum()/valsA.size()+"\t"+valsB.sum()/valsB.size());
  }
}
