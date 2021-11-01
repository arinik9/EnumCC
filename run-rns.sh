#ant -buildfile build-rns.xml compile jar;

inputFilePath="in/net.G"
outDir="out/net/1"
#for filename in `ls $inputDir | sort -V` ; do
#    echo $inputDir"/""$filename"

	#modifiedName=${name//.G/}
    #outDir="out"/"$modifiedName"
    #echo $outDir
    #if [ -d $outDir ]; then
    #  rm -r $outDir
    #fi
    #mkdir $outDir

    rm "out/net/allResults.txt"
    touch "out/net/allResults.txt"
    rm -r "out/net/1"
    mkdir "out/net/1"
    cp "out/net/membership0.txt" "out/net/1"

    ant -v -buildfile build-rns.xml -DinitMembershipFilePath="out/net/membership0.txt" -DallPreviousResultsFilePath="out/net/allResults.txt" -DinputFilePath="$inputFilePath" -DoutDir="$outDir" -DmaxNbEdit=3 -Dtilim=3600 -DsolLim=3000 -DnbThread=1 -DisBruteForce=false -DisIncrementalEditBFS=false run
#done
