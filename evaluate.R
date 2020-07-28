 ############################################################################
 # It reads a .G graph file, and returns the contents as a data frame object.
 #
 # network.path: the file path which stores the .G graph file>
 #
 ############################################################################
 read.graph.ils.file.as.df = function(network.path){
 # skip the first line bc it does not contain graph info
 df = read.table(
 file=network.path, 
 header=FALSE, 
 sep="\t", 
 skip=1, 
 check.names=FALSE
 )
 # df$V1: vertex1
 # df$V2: vertex2
 # df$V3: weight
 return(df)
 }
 
 
 
 ############################################################################
 #  It reads a .G graph file, and returns the contents as a igraph graph object.
 #  To handle isolated nodes, first we had to find the max vertex id.
 #  Then, indicate explicitely vertices ids in graph.data.frame()
 #
 # network.path: the file path which stores the .G graph file
 #
 ############################################################################
 read.graph.ils = function(network.path){
 df = read.graph.ils.file.as.df(network.path)
 
 edg.list = df[,c(1, 2)]
 max.v.id = max(unique(c(edg.list[,1], edg.list[,2])))
 
 g <- graph.data.frame(edg.list, vertices=seq(0,max.v.id), directed=FALSE)
 E(g)$weight = df[, 3]
 # V(g)$id = seq(0,max.v.id)
 # V(g)$id = seq(1,max.v.id+1)
 
 return(g)
 }



 ################################################################################>
 # Loads the partition estimated by the ExCC tool.
 # 
 # file.name: the path and name of the file to load.
 #
 # returns: the corresponding partition as a membership vector.
 ###############################################################################
 load.ExCC.partition <- function(part.folder, result.filename)
 {
     ExCC.output.file <- file.path(part.folder, result.filename)
     #if(algo.name == COR.CLU.ExCC) TODO
     #ExCC.output.file <- file.path(part.folder, "ExCC-result.txt")
     
     # open and read the file
     #print(file.name)
     con <- file(ExCC.output.file, "r")
     lines <- readLines(con)
     close(con)
     #print("---")
     #print(lines)
     
     # process the file content
     i <- 1
     line <- lines[i]
     res <- list()
     
     # TODO: change here if the result file has more information than just the partition
     # in that case, put this line: while(line!="")
     while(!is.na(line)) # line!=""
     {  # process current line
         #print(line)
         line <- strsplit(x=line, "[", fixed=TRUE)[[1]][2]
         line <- strsplit(x=line, "]", fixed=TRUE)[[1]][1]
         
         # we increment by 1 at the end because C++ starts counting from 0
         nodes <- as.integer(strsplit(x=line,", ", fixed=TRUE)[[1]]) + 1
         
         res[[length(res)+1]] <- nodes
         
         # process next line
         i <- i + 1
         line <- lines[i]  
     }
     
     
     # build the membership vector
     mx <- max(unlist(res))
     membership <- rep(NA,mx)
     for(i in 1:length(res))
     {  nodes <- res[[i]]
     membership[nodes] <- i 
     }
     
     #print(membership)
     return(membership)
 }


################################################################################>
#
#
################################################################################>
countDiversityForEdgeVariables = function(membership1,membership2){
    n = length(membership1)
    counter=0
    for(i in 1:(n-1)){
        for(j in (i+1):n){
            #cat("i:",i,", j:",j,"\n")
            if(membership1[i]==membership1[j] && membership2[i]!=membership2[j])
                counter=counter+1
            if(membership1[i]!=membership1[j] && membership2[i]==membership2[j])
                counter=counter+1
        }
    }
    #print(counter)
    norm.term = n*(n-1)/2
    return(counter/norm.term)
}


retreive.distance.matrix.by.measure = function(part.folder, measure){
    mbrshp.files = list.files(path = part.folder, pattern = paste0("^", "membership", ".*\\.txt$")) # filenames are not ordered properly
    nb.sol = length(mbrshp.files)
    m = matrix(NA,nb.sol,nb.sol)
    for(i in 0:(nb.sol-2)){
        for(j in (i+1):(nb.sol-1)){
            membrshp1 = read.table(file.path(part.folder,paste0("membership",i,".txt")))$V1
            membrshp2 = read.table(file.path(part.folder,paste0("membership",j,".txt")))$V1
            if(measure == "binary-hamming"){
                m[i+1,j+1] = round(countDiversityForEdgeVariables(membrshp1,membrshp2),3)
                m[j+1,i+1] = round(countDiversityForEdgeVariables(membrshp2,membrshp1),3)
            }
            else if(measure == "hamming"){
                rel.membrshp2 = create.relative.plot.membership(membrshp1, membrshp2)
                m[i+1,j+1] = hamming.distance(membrshp1,rel.membrshp2)
                rel.membrshp1 = create.relative.plot.membership(membrshp2, membrshp1)
                m[j+1,i+1] = hamming.distance(membrshp2,rel.membrshp1)
            }

        }
    }
    rownames(m)=paste0("sol",seq(0,nb.sol-1))
    colnames(m)=paste0("sol",seq(0,nb.sol-1))
    return(m)
}




library(e1071) # hamming distance
source("~/eclipse/workspace-neon/Sosocc/src/evaluate-partitions/evaluate-imbalance.R")

#part.folder="/media/nejat/TOSHIBA EXT/0CC-Opti-Heur-Analysis/out/partitions/n=32_k=2_dens=1.0000/propMispl=0.7000/propNeg=0.5161/network=3/ExCC-all/signed-unweighted"
part.folder="/media/nejat/TOSHIBA EXT/0CC-Opti-Heur-Analysis/out/partitions/n=28_k=4_dens=1.0000/propMispl=0.3500/propNeg=0.7778/network=3/ExCC-all/signed-unweighted"
##part.folder="/media/nejat/TOSHIBA EXT/0CC-Opti-Heur-Analysis/out/partitions/n=28_k=2_dens=1.0000/propMispl=0.4500/propNeg=0.5185/network=7/ExCC-all/signed-unweighted"
## /sshfs/Sosocc/in/random-networks/n=36_l0=2_dens=0.2500/propMispl=0.5000/propNeg=0.6000/network=1/


source("~/eclipse/workspace-neon/Sosocc/src/cluster-analysis/define-purity.R")
source("~/eclipse/workspace-neon/Sosocc/src/evaluate-partitions/create-relative-plot-membership.R")



m = retreive.distance.matrix.by.measure(part.folder,"binary-hamming")
print(m)
m = retreive.distance.matrix.by.measure(part.folder,"hamming")
print(m)
nb.sol = nrow(m)

# ===================


g = read.graph.ils("in/net.G")
mem1 = load.ExCC.partition("out/net/1","ExCC-result.txt")
sol.mem1 = NA;
for(i in 0:(nb.sol-1)){mem=read.table(file.path(part.folder,paste0("membership",i,".txt")))$V1; if(compare(mem,mem1,"vi")==0){ cat("mem1 belongs to: sol",i,"\n"); sol.mem1=i; break;}}
print(compute.imbalance.from.membership(g,mem1))


mem2 = load.ExCC.partition("out/net/2","ExCC-result.txt")
sol.mem2 = NA; 
for(i in 0:(nb.sol-1)){mem=read.table(file.path(part.folder,paste0("membership",i,".txt")))$V1; if(compare(mem,mem2,"vi")==0){ cat("mem2 belongs to: sol",i,"\n"); sol.mem2=i; break;}}
print(compute.imbalance.from.membership(g,mem2))


mem3 = load.ExCC.partition("out/net/3","ExCC-result.txt")
sol.mem3 = NA
for(i in 0:(nb.sol-1)){mem=read.table(file.path(part.folder,paste0("membership",i,".txt")))$V1; if(compare(mem,mem3,"vi")==0){ cat("mem3 belongs to: sol",i,"\n"); sol.mem3=i; break}}
print(compute.imbalance.from.membership(g,mem3))

#d = hamming.distance(mem1,mem2)
#cat("hamming dist betw mem1 and mem2:",d,"\n")
#d = hamming.distance(mem1,mem3)
#cat("hamming dist betw mem1 and mem3:",d,"\n")
#d = hamming.distance(mem2,mem3)
#cat("hamming dist betw mem2 and mem3:",d,"\n")

cat("mem1(sol",sol.mem1,") vs mem2(sol",sol.mem2,"):",countDiversityForEdgeVariables(mem1,mem2),"\n")
cat("mem1(sol",sol.mem1,") vs mem3(sol",sol.mem3,"):",countDiversityForEdgeVariables(mem1,mem3),"\n")
cat("mem2(sol",sol.mem2,") vs mem3(sol",sol.mem3,"):",countDiversityForEdgeVariables(mem2,mem3),"\n")






