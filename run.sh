
for filename in `ls in/*.G | sort -V` ; do
	name=${filename##*/}
	modifiedName=${name//.G/}

    echo $filename
    echo "in/""$modifiedName"

    outFolderName="out/""$modifiedName""-vertex"
    initMembershipFilePath="$outFolderName/membership0.txt"
    LPFilePath="in/strengthedModel_vertex.lp"
    #mkdir -p $outFolderName
	    
	    
	# Note that user must be aware if the LP file in input has the complete set of triangle constraints or its reduced form. If it is the 2nd one, then one needs to set "triangleIneqReducedForm=true".
    # This parameter is just needed for retrieving the membership info, not for other tasks. >> TODO: we can infer this information directly from LP file by counting the nb triangle constraints
	    
    ant -v -buildfile build.xml -DformulationType="vertex" -DinFile="in/""$name" -DoutDir="$outFolderName" -DmaxNbEdit=3 -DlazyCB=false -DuserCutCB=false -DinitMembershipFilePath="$initMembershipFilePath" -DLPFilePath="$LPFilePath" -DJAR_filepath_RNSCC="RNSCC.jar" -DnbThread=4 -Dtilim=-1 -DsolLim=50000 -DtriangleIneqReducedForm=false -DlazyCB=false -DuserCutCB=false run
    

    # ==============================================================================================

    outFolderName="out/""$modifiedName""-edge"
    initMembershipFilePath="$outFolderName/membership0.txt"
    LPFilePath="in/strengthedModel_edge.lp"
    #mkdir -p $outFolderName
	    
    #ant -v -buildfile build.xml -DformulationType="edge" -DinFile="in/""$name" -DoutDir="$outFolderName" -DmaxNbEdit=3 -DlazyCB=true -DuserCutCB=false -DinitMembershipFilePath="$initMembershipFilePath" -DLPFilePath="" -DJAR_filepath_RNSCC="RNSCC.jar" -DnbThread=4 -Dtilim=-1 -DsolLim=50000 run

done
