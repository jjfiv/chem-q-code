# chem-q-code
Code from [Retrieving Hierarchical Syllabus Items for Exam Question Analysis](http://ciir-publications.cs.umass.edu/pub/web/getpdf.php?id=1190) even though data is still private.

# Checking out:

    # Add --recursive to get dependency submodules.
    git clone --recursive https://github.com/jjfiv/chem-q-code.git
    
# Build and install dependencies (once):

    ./install_deps.sh

# Build this code:

    mvn install

# Check out RunExperiment (the class) to build off this code.

Because our hierachy was so small, (and we played with expansion) RunExperiment indexes all data needed, adding in Chemistry StackExchange questions as needed/requested. The code is a bit of a mess because the key challenge was data cleaning.

- The overall flow is in the [RunExperiment](https://github.com/jjfiv/chem-q-code/blob/master/src/main/java/ciir/yggdrasil/chemistry/experiments/RunExperiment.java#L48) class.
- For indexing and loading the hierarchy, check out [ExperimentResources](https://github.com/jjfiv/chem-q-code/blob/master/src/main/java/ciir/yggdrasil/chemistry/experiments/ExperimentResources.java#L214).
- Fairly clean implementations of models ended up in [ciir.yggdrasil.chemistry.experiments.method](https://github.com/jjfiv/chem-q-code/tree/master/src/main/java/ciir/yggdrasil/chemistry/experiments/method), e.g., [HierarchicalSDM](https://github.com/jjfiv/chem-q-code/blob/master/src/main/java/ciir/yggdrasil/chemistry/experiments/method/HierarchicalSDM.java)

The packages are named after Yggdrasil, which is a tree from norse mythology, since the goal of this work is to classify questions into trees.
