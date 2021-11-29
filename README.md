# EnumCC
Efficient Enumeration of Correlation Clustering Optimal Solution Space

* Copyright 2020-21 Nejat Arınık

*EnumCC* is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation. For source availability and license information see the file `LICENCE`

* GitHub repo: https://github.com/arinik9/EnumCC
* Contact: Nejat Arınık <arinik9@gmail.com>



### Description

*EnumCC* is an optimal solution space enumeration method for the *Correlation Clustering (CC)* problem. It relies on two essential tasks: Recurrent neighborhood search (*RNS*) and *jumping* onto an undiscovered solution. The former is performed by the component *RNSCC*, whereas the latter is done by the commercial solver *Cplex*.

In the first step, instead of directly *jumping* onto undiscovered optimal solutions one by one through Cplex, as in a traditional sequential approach, its component *RNSCC* discovers the recurrent neighborhood of the current optimal solution *P* with the hope of discovering new optimal solutions. The recurrent neighborhood of an optimal solution *P*, represents the set of optimal solutions, reached directly or indirectly from *P* depending on the maximum distance parameter *maxNbEdit*. Whether a new solution is found or not through *RNSCC*, the jumping process into a new solution *P* is performed. If *P* is not empty, the workflow of  *RNS* and *jumping* is repeated again. Otherwise, the enumeration process stops. See Chapter 5 of *[Arınık'21]* for more details.

*EnumCC* performs the enumeration process based on two different formulation types: 1) decision variables defined on vertex-pair (*Fv*: "*vertex*" formulation type) or 2) edge (*Fe*: "*edge*" formulation type). If we denote "*n*" by the number of vertices in the graph and "*m*" by the number of edges, there are *(n(n-1)/2)* variables in *Fv*, whereas there are *m* variables in *Fe*. 

So, which one would be preferable over the other one? We have not answered this question yet. However, some small experiments show that the resolution method relying on the *Fv* formulation is in general more efficient, especially if *EnumCC* needs to perform a large number of "*jumps*". Because, the  resolution method relying on the *Fe* formulation performs these jumps through lazy callback, and this can worsen the performance.

 Another remark of performance issue concerns the "*triangleIneqReducedForm*" parameter, which concerns only the *Fv* formulation. See the description of this parameter for more information.



### Input parameters

 * **formulationType:** ILP formulation type. Either *"vertex"* for *Fv* or *"edge"* for *Fe*.
* **inFile:** Input file path. See *in/exemple.G* for the input graph format. 
 * **outDir:** Output directory path. Default "." (i.e. the current directory).
 * **initMembershipFilePath:** The membership file path, from which the *RNSCC* starts. It must be an optimal solution of the given signed graph. Moreover, It must be named as *membership0.txt* or something different than *membership<x>.txt*. See *out/exemple/membership0.txt* for its format. This file can be obtained through  [ExCC](https://github.com/arinik9/ExCC) by running the script `run-cp-bb.sh`.
 * **java.library.path:** The Cplex java library path. It is usually found in *<YOUR_CPLEX_PATH>/cplex/lib/cplex.jar*.
 * **maxNbEdit** The maximum value edit distance value to be considered in edit operations. We show in our experiments that *maxNbEdit=3* is usually more appropriate. 
 * **tilim:** Time limit in seconds for the whole program.Default *-1*, which means no time limit.
 * **solLim**  Maximum number of optimal solutions to be discovered. This can be useful when there is a huge number of optimal solutions, e.g. 50,000. Default *-1*.
 * **JAR_filepath_RNSCC:** The jar file path for *RNSCC*.
 * **LPFilePath** It allows to import a Cplex LP file, corresponding to a ILP formulation of a signed graph for the CC problem. *Remark:* Such a file is obtained through Cplex by doing *exportModel()*. This file can be obtained through  [ExCC](https://github.com/arinik9/ExCC) by running the script `run-cp-bb.sh`. In *ExCC*, the name of this file is  *strengthedModelAfterRootRelaxation.lp.
 * **lazyInBB:** Used only for the Fe formulation type. True if adding lazily triangle constraints (i.e. lazy callback approach) is used during the branching phase when trying to find a new optimal solution. Default false.
 * **userCutInBB:** True if adding user cuts during the branching phase is desired, when trying to find a new optimal solution. Based on our experiments, we can say that it does not yield any advantage, and it might even slow down the optimization process. Default false.

* **triangleIneqReducedForm :** Used only for the *Fv* formulation type. It requires the knowledge of the user. Because, when it is set to true, this indicates that the parameter "*LPFilePath*" contains the reduced number ("non-redundant") of triangle inequalities in the formulation. It is important to know this information, because the way the partition information is extracted from an optimal solution is slightly different. This extraction step is explained in *[Miyaichi'18]*. Default value is false, which keeps the whole set of triangle constraints. It is worth noting that removing redundant triangle inequalities from the ILP formulation can be very beneficial for complete or very dense signed graphs, when enumerating all optimal solutions. See some small experiments conducted in *Chapter 2* of *[Arınık'21]* based on complete random signed networks. However, removing such inequalities from the ILP formulation in sparse signed graphs can substantially worsen the performance of this optimization process. This is because Cplex can find many optimal solutions which actually correspond to a same optimal partition.

### Instructions & Use

#### Use 1

* Install [`IBM CPlex`](https://www.ibm.com/docs/en/icos/20.1.0?topic=2010-installing-cplex-optimization-studio). The default installation location is: `/opt/ibm/ILOG/CPLEX_Studio<YOUR_VERSION>`. Tested with Cplex 12.8 and 20.1.
* Put `/opt/ibm/ILOG/CPLEX_Studio<YOUR_VERSION>/cplex/lib/cplex.jar` into the `lib` folder in this repository.
* Compile and get the jar file for *RNSCC*: `ant -v -buildfile build-rns.xml compile jar`.
* Compile and get the jar file for *EnumCC* `ant -v -buildfile build.xml compile jar`.
* We need a starting optimal solution and the ILP model of the given signed graph. We can obtain them by running the script `run-cp-bb.sh` in the [ExCC](https://github.com/arinik9/ExCC) repository.
* Run the script `run.sh`.


#### Use 2

* Put `/opt/ibm/ILOG/CPLEX_Studio<YOUR_VERSION>/cplex/lib/cplex.jar` into the `lib` folder in this repository.
* Compile and get the jar file for *RNSCC*: `ant -v -buildfile build-rns.xml compile jar`.
* Compile and get the jar file for *EnumCC*: `ant -v -buildfile build.xml compile jar`.
* Download the [BenchmarkCC](https://github.com/arinik9/BenchmarkCC) repository and put these jar files into the `lib` folder. Then, run the program for the *ExCC* and *EnumCC(3)* methods. See the instructions of the *BenchmarkCC* repository for more details.



**Example command for the *Fv* formulation:**

```bash
ant -v -buildfile build.xml compile jar;
ant -v -buildfile build.xml -DformulationType="vertex" -DinFile="in/example.G" -DoutDir="out/example-vertex" -DmaxNbEdit=3 -DinitMembershipFilePath="out/example-vertex/membership0.txt" -DLPFilePath="in/strengthedModel_vertex.lp" -DJAR_filepath_RNSCC="RNSCC.jar" -DnbThread=4 -Dtilim=-1 -DsolLim=5000 -DtriangleIneqReducedForm=false -DlazyCB=false -DuserCutCB=false run
```

**Example command for the *Fe* formulation:**

```bash
ant -v -buildfile build.xml compile jar;
ant -v -buildfile build.xml -DformulationType="edge" -DinFile="in/example.G" -DoutDir="out/example-edge" -DmaxNbEdit=3 -DinitMembershipFilePath="out/example-edge/membership0.txt" -DLPFilePath="in/strengthedModel_edge.lp" -DJAR_filepath_RNSCC="RNSCC.jar" -DnbThread=4 -Dtilim=-1 -DsolLim=5000 -DtriangleIneqReducedForm=false -DlazyCB=true -DuserCutCB=false run
```

### Output

* **<x>**: Folder *<x>*, where <x> is a numerical value starting from 1. Each folder contains the result of a *RNSCC* execution and possesses one or multiple optimal solutions.

* **allResults.txt** File storing all absolute paths of the discovered optimal solutions.

* **exec-time.txt** Execution time for the whole enumeration process.

* **jump-exec-time<x>.txt** Execution time for the *x*.th jumping process through Cplex.

* **jump-log<x>.txt** Cplex log file regarding the the *x*.th jumping process.

* **jump-status<x>.txt** The Cplex status result in the end of the jumoing process. Three values are possible: *Optimal*, *SolLim*, *Infeasible*

* **membership<x>.txt** The starting membership file for the (*x+1*).th *RNSCC* process.

  


# References
* **[Arınık'21]** N. Arınık, [*Multiplicity in the Partitioning of Signed Graphs*](https://www.theses.fr/2021AVIG0285). PhD thesis in Avignon Université (2021).
* **[Miyauchi'18]** A. Miyauchi, T. Sonobe, and N. Sukegawa,  *Exact Clustering via Integer Programming and Maximum Satisfiability*, in: AAAI Conference on Artificial Intelligence 32.1 (2018).
