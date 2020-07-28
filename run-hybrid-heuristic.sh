#ant -buildfile build-hybrid-heuristic.xml compile jar;

inputDir="in"
graphFileName="net.G"
outDir="out/net"
startSolutionDirPath="out/net"
#for filename in `ls $inputDir | sort -V` ; do
#    echo $inputDir"/""$filename"

	#modifiedName=${name//.G/}
    #outDir="out"/"$modifiedName"
    #echo $outDir
    #if [ -d $outDir ]; then
    #  rm -r $outDir
    #fi
    #mkdir $outDir

    ant -buildfile build-hybrid-heuristic.xml -Djava.library.path="/opt/ibm/ILOG/CPLEX_Studio128/cplex/bin/x86-64_linux" -DinputDirPath="$inputDir" -DgraphFileName="$graphFileName" -DstartSolutionDirPath="$startSolutionDirPath" -DoutDir="$outDir" -DmaxNbEdit=3 -DnbRepetition=1 run
#done
