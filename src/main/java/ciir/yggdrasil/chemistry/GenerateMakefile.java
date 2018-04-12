package ciir.yggdrasil.chemistry;

import ciir.jfoley.chai.collections.util.IterableFns;
import ciir.jfoley.chai.collections.util.ListFns;
import org.lemurproject.galago.tupleflow.FileUtility;
import org.lemurproject.galago.tupleflow.Utility;
import org.lemurproject.galago.utility.Parameters;
import org.lemurproject.galago.utility.tools.AppFunction;

import java.io.File;
import java.io.PrintStream;
import java.util.*;

/**
 * @author jfoley.
 */
public class GenerateMakefile extends AppFunction {
  public static String header = "" +
    "SHELL:=/bin/bash\n" +
    "JAR:=target/yggdrasil-1.0-SNAPSHOT.jar\n" +
    "JAVA:=java\n" +
    "CHEMISTRY_STACKEXCHANGE_DATA:='/home/jfoley/Downloads/chemistry.stackexchange.com/'\n" +
    ".PHONY: jar\n" +
    "jar:\n" +
    "\tmvn package\n\n";

  /** Returns output or target of the rule */
  public static void printRule(PrintStream output, String target, List<String> deps, String rule) {
    output.printf("%s: %s\n\t%s\n\n", target, Utility.join(deps, " "), rule);
  }

  @Override
  public String getName() {
    return "generate-makefile";
  }

  @Override
  public String getHelpString() {
    return makeHelpStr("args", "N/A");
  }

  public static List<Parameters> RFSettings = Arrays.asList(
    Parameters.parseArray(
    ),
    Parameters.parseArray(
      "rf", "srf"
    ),
    Parameters.parseArray(
      "rf", "hsrf"
    ),
    Parameters.parseArray(
      "rf", "hrf"
    ));

  public static String toArguments(Parameters args) {
    StringBuilder sb = new StringBuilder();
    for (String key : IterableFns.sorted(args.keySet())) {
      if(args.isString(key)) {
        sb.append(" --").append(key).append('=').append(args.getString(key));
      } else if(args.isList(key)) {
        List objs = args.getList(key);
        for (Object obj : objs) {
          sb.append(" --").append(key).append('+').append(obj.toString());
        }
      } else if(args.isLong(key) || args.isBoolean(key)) {
        sb.append(" --").append(key).append('=').append(args.getAsString(key));
      } else {
        throw new RuntimeException("Not handled: " + args.get(key));
      }
    }
    return sb.toString().substring(1);
  }

  public static List<Parameters> combinations(List<Parameters> a, List<Parameters> b) {
    List<Parameters> output = new ArrayList<>(a.size() * b.size());
    for (Parameters ai : a) {
      for (Parameters bi : b) {
        Parameters c = Parameters.create();
        c.copyFrom(ai);
        c.copyFrom(bi);
        output.add(c);
      }
    }
    return output;
  }

  public static List<Parameters> combinations(List<Parameters>... data) {
    List<List<Parameters>> ys = Arrays.asList(data);
    if(ys.isEmpty()) {
      return Collections.emptyList();
    } else if(ys.size() == 1) {
      return ys.get(0);
    }

    // reduce.
    List<Parameters> accum = ys.get(0);
    for (int i = 1; i < ys.size(); i++) {
      accum = combinations(accum, ys.get(i));
    }

    return accum;
  }

  public static List<Parameters> downsampleOpts() {
  List<Parameters> output = new ArrayList<>();
    // no downsampling...
    output.add(Parameters.create());
    for(int numExams : Arrays.asList(1,2,3,4,5,6,7,8,9,10,13,15,17,20,23)) {
      for(int randomSeed : Arrays.asList(13,42,123)) {//,554,4096,0xdeadbeef,7,8,9,10)) {
        output.add(Parameters.parseArray(
            "downsample", numExams,
            "seed", randomSeed
        ));
      }
    }
    return output;
  }

  public static List<Parameters> makeKNNExperiments(Parameters argp) {
    List<Parameters> knnMethod = Arrays.asList(Parameters.parseArray(
            "method", "question-knn") /*,
        Parameters.parseArray(
            "method", "question-knn",
            "question-context-model", true)*/
    );

    List<Parameters> depthMethods = new ArrayList<>();
    List<Parameters> aggregateMethods;
    if(argp.get("explore-knn-methods", false)) {
      aggregateMethods = Arrays.asList(
        Parameters.parseArray("knnVotingMethod", "ranksum"),
        Parameters.parseArray("knnVotingMethod", "scoresum"),
        Parameters.parseArray("knnVotingMethod", "scorewsum"),
        Parameters.parseArray("knnVotingMethod", "count")
      );

      for (int knnDepth : Arrays.asList(1, 5, 10, 25, 50, 100)) {
        depthMethods.add(Parameters.parseArray(
          "knnDepth", knnDepth
        ));
      }
    } else {
      aggregateMethods = Collections.singletonList(Parameters.parseArray("knnVotingMethod", "ranksum"));
      depthMethods.add(Parameters.parseArray("knnDepth", 50));
    }

    //List<Parameters> experimentsSansNodes = combinations(knnMethod, downsampleOpts(), aggregateMethods, depthMethods);
    List<Parameters> experimentsSansNodes = combinations(knnMethod, aggregateMethods, depthMethods);
    for (Parameters exp : experimentsSansNodes) {
      exp.put("knnIncludeHierarchy", false);
    }

    // With nodes, we can consider hierarchical, and expansion.
    List<Parameters> experimentsWithNodes = new ArrayList<>();
    for (Parameters exp : experimentsSansNodes) {
      Parameters exp2 = exp.clone();
      exp2.put("knnIncludeHierarchy", true);
      experimentsWithNodes.add(exp2);
    }

    return ListFns.lazyConcat(experimentsSansNodes, experimentsWithNodes);
  }

  @Override
  public void run(Parameters argp, PrintStream output) throws Exception {
    output.println(header);

    List<String> runsGenerated = new ArrayList<>();

    List<Parameters> expansionSettings = new ArrayList<>();
    // no expansion
    expansionSettings.add(Parameters.create());
    List<Integer> nodeExpansionList = Arrays.asList(1,5,10,25,50,100);
    if(!argp.get("node-expansion-experiment", false)) {
      //nodeExpansionList = Collections.singletonList(50); // best value determined on unsupervised.
      nodeExpansionList = Arrays.asList(50); // best value determined on unsupervised.
    }

    for (int depth : nodeExpansionList) {
      expansionSettings.add(Parameters.parseArray(
        "node-expansion-depth", depth
      ));
    }

    List<Parameters> methods = new ArrayList<>();
    methods.add(Parameters.parseArray("method", "unsupervised"));
    //methods.add(Parameters.parseArray("method", "unsupervised", "question-context-model", true));
    //methods.add(Parameters.parseArray("method", "hierarchical-sdm", "question-context-model", true));
    methods.add(Parameters.parseArray("method", "hierarchical-sdm"));
    //methods.add(Parameters.parseArray("method", "hierarchical-sdm", "expand-descendants", false));

    List<Parameters> nodeMethods = combinations(RFSettings, expansionSettings, methods);
    //List<Parameters> nodeMethods = combinations(expansionSettings, methods);
    System.err.println("# Unsupervised, all settings: " + nodeMethods.size());

    for (Parameters settings : nodeMethods) {
      String target = generateJob(output, settings);
      runsGenerated.add(target);
    }
    for(Parameters settings : combinations(
      RFSettings,
      Arrays.asList(Parameters.parseArray("question-context-model", true), Parameters.create()),
      Arrays.asList(Parameters.parseArray("skipHierarchy", true), Parameters.create()),
      Collections.singletonList(Parameters.parseArray("method", "supervised-expansion")))) {
      System.err.println(settings);
      String target = generateJob(output, settings);
      runsGenerated.add(target);
    }

    List<Parameters> knnSupervised = makeKNNExperiments(argp);
    System.err.println("# KNN-supervised, all settings: " + knnSupervised.size());
    for (Parameters settings : knnSupervised) {
      String target = generateJob(output, settings);
      runsGenerated.add(target);
    }

    runsGenerated.add(generateJob(output,
        Parameters.parseArray(
            "method", "liblinear"
        )));

    assert(runsGenerated.size() == (new HashSet<>(runsGenerated)).size()) : "Require unique names for all jobs.";

    List<String> additionalRuns = new ArrayList<>();
    for (File file : FileUtility.safeListFiles(new File("runs/"))) {
      additionalRuns.add(file.toString());
    }

    Set<String> uniqRuns = new HashSet<>();
    uniqRuns.addAll(runsGenerated);
    uniqRuns.addAll(additionalRuns);

    Parameters runTexTableJob = Parameters.parseArray(
        "output", "runs.tex",
        "fn", "print-table",
        "runs", IterableFns.sorted(uniqRuns)
    );
    printRule(output, runTexTableJob.getString("output"), IterableFns.sorted(uniqRuns),
      String.format("${JAVA} -ea -jar ${JAR} %s > %s", toArguments(runTexTableJob), runTexTableJob.getString("output")));
  }

  public String generateJob(PrintStream output, Parameters settings) {
    StringBuilder target = new StringBuilder();
    // put everything in a runs/ subfolder.
    target.append("runs/");

    target.append(settings.getString("method"));
    if(settings.get("question-context-model", false)) {
      target.append(".qcm");
    }
    if (settings.isLong("downsample")) {
      target.append(".only").append(settings.getLong("downsample")).append("exams");
    }
    if(settings.isLong("seed")) {
      target.append(".rand").append(settings.getLong("seed"));
    }
    if(!settings.get("expand-descendants", true)) {
      target.append(".no-tree");
    }
    if (settings.isString("rf")) {
      target.append('.').append(settings.getString("rf"));
    }
    if (settings.get("node-expansion-depth", 0) > 0) {
      target.append(".ne").append(settings.getLong("node-expansion-depth"));
    }
    if (settings.get("knnIncludeHierarchy", false)) {
      target.append(".includeNodes");
    }
    if (settings.get("skipHierarchy", false)) {
      target.append(".skipH");
    }
    if (settings.isLong("knnDepth") ) {
      target.append(".k").append(settings.getLong("knnDepth"));
    }
    if (settings.isString("knnVotingMethod") ) {
      target.append(".").append(settings.getString("knnVotingMethod"));
    }
    target.append(".json");

    Parameters job =
      Parameters.parseArray(
        "fn", "run-experiment",
        "output", target.toString()
      );
    job.copyFrom(settings);

    printRule(output,
      job.getString("output"),
      Collections.<String>emptyList(),
      String.format("${JAVA} -ea -jar ${JAR} %s", toArguments(job))
    );
    return target.toString();
  }

  public static void main(String[] args) throws Exception {
    AppFunction fn = new GenerateMakefile();
    fn.run(Parameters.parseArgs(args), System.out);
  }
}
