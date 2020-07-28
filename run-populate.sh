#ant -buildfile build-populate.xml compile jar;

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

    ant -v -buildfile build-populate.xml -Djava.library.path="/opt/ibm/ILOG/CPLEX_Studio128/cplex/bin/x86-64_linux" -DinputDirPath="$inputDir" -DgraphFileName="$graphFileName" -DstartSolutionDirPath="$startSolutionDirPath" -DoutDir="$outDir" -DmaxNbEdit=4 -Dtilim=3600 -DnbThread=6 run
#done
