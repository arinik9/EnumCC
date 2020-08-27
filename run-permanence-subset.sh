#ant -buildfile build-permanence-subset.xml clean compile jar;

inputDir="in"
graphFileName="net.G"
startSolutionDirPath="out/net"

ant -v -buildfile build-permanence-subset.xml -DinputDirPath="$inputDir" -DgraphFileName="$graphFileName" -DinputMembership="[1,1,2,2,1]" -DnbEdit=4 -DsubsetMaxSize=12 -DmaxNbDistinctDeltaFitnessForPossibleTargetClusters=3 run

#done
