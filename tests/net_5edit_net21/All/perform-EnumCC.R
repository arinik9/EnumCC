source("permanence-analysis.R")

`%notin%` <- Negate(`%in%`)


##################################################
#
#
##################################################
is.split.from.single.cluster = function(input.membership, rel.output.membership, moving.nodes){
	k1 = length(unique(input.membership))
	k2 = length(unique(rel.output.membership))
	if(abs(k1-k2)!=1)
		return(FALSE)
	
	all.together1 = length(unique(input.membership[moving.nodes]))==1
	all.together2 = length(unique(rel.output.membership[moving.nodes]))==1

	if(all.together1 && all.together2)
		return(TRUE)
	return(FALSE)
}


##################################################
# sometimes, there is no split operation from single cluster. This is why we cover this case.
# basically, if this is a 5-edit operation, we try to find one 4-edit operation which is a split from a single cluster (i.e. tolerance for 1 node)
#
##################################################
is.almost.split.from.single.cluster = function(input.membership, rel.output.membership, moving.nodes){
    nb.moving.nodes = length(moving.nodes)
    for(i in 1:nb.moving.nodes){
        moving.nodes2 = moving.nodes[-i]
	    all.together1 = length(unique(input.membership[moving.nodes2]))==1
	    all.together2 = length(unique(rel.output.membership[moving.nodes2]))==1

	    if(all.together1 && all.together2)
		    return(TRUE)
    }
	return(FALSE)
}


##################################################
#
#
##################################################
is.operation.from.diff.clusters = function(input.membership, moving.nodes){
    nb.moving.nodes = length(moving.nodes)
	all.diff = length(unique(input.membership[moving.nodes]))==nb.moving.nodes

	if(all.diff)
		return(TRUE)
	return(FALSE)
}


##################################################
# sometimes, there is no split operation from single cluster. This is why we cover this case.
# basically, if this is a 5-edit operation, we try to find one 4-edit operation which is a split from a single cluster (i.e. tolerance for 1 node)
#
##################################################
is.operation.from.almost.diff.clusters = function(input.membership, moving.nodes){
    nb.moving.nodes = length(moving.nodes)
    for(i in 1:nb.moving.nodes){
        moving.nodes2 = moving.nodes[-i]
	    all.diff = length(unique(input.membership[moving.nodes2]))==(nb.moving.nodes-1)

	    if(all.diff)
		    return(TRUE)
    }
	return(FALSE)
}



##################################################
#
#
##################################################
is.connected.moving.nodes = function(g, input.membership, output.membership, moving.nodes){
    if(length(unique(input.membership[moving.nodes]))==1 || length(unique(output.membership[moving.nodes]))==1)
        return(TRUE)

    nb.edit = length(moving.nodes)
    new.ids = seq(1, nb.edit)
    names(new.ids) = as.character(moving.nodes)

    target.cluster.node.assoc.list = list()
    source.cluster.node.assoc.list = list()
    for(mnode in moving.nodes){
        target.clu.id.str = as.character(output.membership[mnode])
        target.cluster.node.assoc.list[[target.clu.id.str]] = c(target.cluster.node.assoc.list[[target.clu.id.str]], mnode)
        source.clu.id.str = as.character(input.membership[mnode])
        source.cluster.node.assoc.list[[source.clu.id.str]] = c(source.cluster.node.assoc.list[[source.clu.id.str]], mnode)
    }

    B = matrix(0, nb.edit, nb.edit)
    for(mnode in moving.nodes){
        target.clu.id.str = as.character(output.membership[mnode])
        source.clu.id.str = as.character(input.membership[mnode])
        if(length(source.cluster.node.assoc.list[[source.clu.id.str]])>0){
            ids = source.cluster.node.assoc.list[[source.clu.id.str]]
            ids = ids[-which(ids == mnode)]
            ids = new.ids[as.character(ids)]

            ids = ids[-which(ids == mnode)]
            if(length(ids)>0){
                B[new.ids[as.character(mnode)],ids]=1
                B[ids,new.ids[as.character(mnode)]]=1
            }
        }

        if(length(target.cluster.node.assoc.list[[target.clu.id.str]])>0){
            ids = target.cluster.node.assoc.list[[target.clu.id.str]]
            ids = ids[-which(ids == mnode)]
            ids = new.ids[as.character(ids)]
            if(length(ids)>0){
                B[new.ids[as.character(mnode)],ids]=1
                B[ids,new.ids[as.character(mnode)]]=1
            }
        }
    }

    g2 = graph_from_adjacency_matrix(adjmatrix=B, mode="undirected")
    nb.comp = count_components(g2)
    if(nb.comp!=1)
        return(FALSE)
    return(TRUE)
}


##################################################
#
#
##################################################
is.positive.connected.internal.moving.nodes = function(g, membership, moving.nodes){
    moving.membership = membership[moving.nodes]
    ok = TRUE
    for(clu.id in unique(moving.membership)){
        sub.moving.nodes = moving.nodes[which(moving.membership == clu.id)]
        if(length(sub.moving.nodes)>1){
            g2 <- induced.subgraph(graph=g,vids=sub.moving.nodes)
            g2pos <- delete.edges(graph=g2,edges=which(E(g2)$weight<0))
            nb.comp = count_components(g2pos)
            if(nb.comp!=1){
                ok = FALSE
                break
            }
        }
    }
    return(ok)
}


##################################################
#
#
##################################################
is.positive.sum.internal.neigh.links = function(g, A, membership, moving.nodes){
    moving.membership = membership[moving.nodes]
    ok = TRUE
    for(clu.id in unique(moving.membership)){
        sub.moving.nodes = moving.nodes[which(moving.membership == clu.id)]
        if(length(sub.moving.nodes)>1){
            row.wise.sum = apply(A[sub.moving.nodes,sub.moving.nodes], 1, sum)
            neg.indxs = which(row.wise.sum<0)
            if(length(neg.indxs)>0){
                ok = FALSE
                break
            }
        }
    }
    return(ok)
}


##################################################
#
##################################################
is.weight.sum.zero.with.neigh.nodes = function(g, A, membership, moving.nodes){
    moving.membership = membership[moving.nodes]
    ok = TRUE
    for(clu.id in unique(moving.membership)){
        sub.moving.nodes = moving.nodes[which(moving.membership == clu.id)]
        B = A[moving.nodes, moving.nodes]
        for(i in 1:length(sub.moving.nodes)){
            if(sum(A[i,-i])==0)
               return(TRUE)
        }
    }
    return(FALSE)
}



#############################################################################################
# It loads all partitions from the membership files whose the name is structured as "membership<XX>.txt" where XX
#   indicates the partition number (e.g. 0, 1, etc.), and then it stores as a list of vectors. 
#   Note that each partition info, i.e. membership, is stored as a vector.
#   Note that a file "membership0.txt" might be like this (each line is associated with a node and each line contains cluster info):
#     1
#     2
#     1
#     3
#     .
#     .
#
# part.folder: the partitioning result folder
#############################################################################################
load.membership.files = function(part.folder) {
    mbrshps = list()
    #paste0("^membership",".*\\.txt$")
    mbrshp.files = list.files(path = part.folder,
                              pattern = paste0("^", MBRSHP.FILE.PREFIX, ".*\\.txt$"))
    nb.mbrshp.file = length(mbrshp.files)
    
    if (nb.mbrshp.file > 0) {
        for (id in 0:(nb.mbrshp.file - 1)) {
            # load the resulting partition file
            table.file = file.path(part.folder, paste0(MBRSHP.FILE.PREFIX, id, ".txt"))
            mbrshp <-
                as.numeric(as.matrix(read.table(
                    file = table.file, header = FALSE
                )))
            mbrshps[[id + 1]] = mbrshp
        }
    }
    
    return(mbrshps)
}

############################################################################
# It reads a .G graph file, and returns the contents as a data frame object.
#
# network.path: the file path which stores the .G graph file
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
	cat("max id: ",max.v.id, "\n")
	E(g)$weight = df[, 3]
	# V(g)$id = seq(0,max.v.id)
	# V(g)$id = seq(1,max.v.id+1)
	
	return(g)
}


############################################################################
# It calculates the imbalance in terms of count or percentage for Correlation Clustering problem.
#
# g: graph
# membership: a membership, i.e. partition vector
# output.type: either "count" or "percentage" or "node.imbalance"
#
# returns imbalance
############################################################################
compute.imbalance.from.membership = function(g, edge.mat, membership, output.type = "count"){

	#membership = as.integer(membership)
	clus.mat <- cbind(membership[edge.mat[,1]], membership[edge.mat[,2]])
		
	neg.links <- E(g)$weight<0
	pos.links <- E(g)$weight>=0

	misplaced <- (clus.mat[,1]==clus.mat[,2] & neg.links) | (clus.mat[,1]!=clus.mat[,2] & pos.links)
	imb.val = sum(abs(E(g)$weight[misplaced]))
	
	
	if(output.type == "count")
        return(imb.val)
		#return(format(round(imb.val, 3), nsmall = 3)) # 3 decimal floating
	else
		return(NA)
	
}




##################################################
#
#
##################################################
is.subedit.operation = function(g, A, edge.mat, input.membership, output.membership, lib.folder, opt.imb.count){
    rel.output.membership = get.relative.membership(input.membership, output.membership, lib.folder)
    moving.nodes = get.moving.nodes(input.membership, rel.output.membership, lib.folder)
    nb.edit = length(moving.nodes)
    #if(nb.edit>3){
        # ------------------------------------------------
        is.pos.conn = is.positive.connected.internal.moving.nodes(g, input.membership, moving.nodes)
        is.pos.sum = is.positive.sum.internal.neigh.links(g, A, input.membership, moving.nodes)
        is.conn = is.connected.moving.nodes(g, input.membership, rel.output.membership, moving.nodes)
        # ------------------------------------------------

        if(is.pos.conn && is.pos.sum && is.conn &&
            !is.weight.sum.zero.with.neigh.nodes(g, A, input.membership, moving.nodes) &&
            !is.weight.sum.zero.with.neigh.nodes(g, A, rel.output.membership, moving.nodes) )
        {
            #         [,1] [,2] [,3] [,4] [,5] [,6]
            #    [1,] "a"  "a"  "a"  "b"  "b"  "c" 
            #    [2,] "b"  "c"  "d"  "c"  "d"  "d"
            for(nb.subedit in 1:ceiling(nb.edit/2)){
                combs = combn(moving.nodes, nb.subedit)
                for(i in 1:ncol(combs)){
                    sub.moving.nodes = combs[,i]
                    temp.output.membership = input.membership
                    temp.output.membership[sub.moving.nodes] = rel.output.membership[sub.moving.nodes]
                    imb.count = compute.imbalance.from.membership(g, edge.mat, temp.output.membership, output.type = "count")
                    if(imb.count == opt.imb.count)
                        return(TRUE)
                }
            }
        }
    #}
    return(FALSE)
}



##################################################
#
#
##################################################
get.relative.membership = function(mem1, mem2, lib.folder){
	n = length(mem1)
	first = paste(mem1, collapse=",")
	second = paste(mem2, collapse=",")
	cmd=paste0("java -DisBatchMode=false -DfirstClusteringString=",first," -DsecondClusteringString=",second," -DisRelativeMembershipAsOutput=true -jar ",lib.folder,"/ClusteringEditDist.jar")
	res = system(command=cmd, intern=T)
	rel.mem2 = as.integer(unlist(strsplit(res, ",")))
	return(rel.mem2)
}



##################################################
#
#
##################################################
get.moving.nodes = function(mem1, mem2, lib.folder){
	n = length(mem1)
	#rel.mem2 = get.relative.membership(mem1, mem2, lib.folder)

	moving.nodes = c()
	for(i in 1:n){
		#if(mem1[i] != rel.mem2[i])
		if(mem1[i] != mem2[i])
			moving.nodes = c(moving.nodes, i) # node id and index is different
	}
	return(moving.nodes)
}


##################################################
#
#
##################################################
filter.new.sol.indexs.by.decomposable.edit = function(g, A, edge.mat, memberships, curr.sol.idx, new.sol.idxs, lib.folder, opt.imb.count){
    undecomposable.new.sol.idxs = c()
    for(new.sol.idx in new.sol.idxs){
        input.membership = memberships[[curr.sol.idx]]
        output.membership = memberships[[new.sol.idx]]
        res = is.subedit.operation(g, A, edge.mat, input.membership, output.membership, lib.folder, opt.imb.count)
        if(!res)
            undecomposable.new.sol.idxs = c(undecomposable.new.sol.idxs, new.sol.idx)
    }
    return(undecomposable.new.sol.idxs)
}


##################################################
# we want to simulate our enumCC algorithm. But, we do not have any knowledge from Cplex when the algo makes some jumps .. so it is not an exact simulation
#
##################################################
# m is the edit dist matrix
perform.my.enum.algo = function(g, A, edge.mat, memberships, m, max.nb.edit, lib.folder, opt.imb.count, start.sol.indx, compute.permanence){
#find.nb.enum.jump = function(g, A, edge.mat, memberships, m, max.nb.edit, lib.folder, opt.imb.count){
    perm.eval.results = list()
    discovered = rep(FALSE,nrow(m))
    counter = 0
    edit.comps = list()
    discovered.ordered.sol.indxs = c()
    while(any(discovered == FALSE)){    
        counter = counter + 1
        cat("counter: ", counter, " => current size: ", length(which(discovered==TRUE)),"\n")
        print(discovered)
        #next.sol.indexs = which(discovered == FALSE)[1]
        if(counter!=1){
            max.indx = which.max(m[start.sol.indx,which(discovered == FALSE)]) 
            start.sol.indx = which(discovered == FALSE)[max.indx]  
        }
        next.sol.indexs = start.sol.indx
        discovered[next.sol.indexs] = TRUE
        discovered.ordered.sol.indxs = c(discovered.ordered.sol.indxs, start.sol.indx)

        while(TRUE){
            results = perform.one.pass(g, A, edge.mat, memberships, next.sol.indexs, discovered, max.nb.edit, lib.folder, opt.imb.count, compute.permanence)
            curr.perm.eval.results = results[["perm.eval.results"]] 
            new.sol.indexs = results[["new.sol.indexs"]]
            discovered.ordered.sol.indxs = c(discovered.ordered.sol.indxs, new.sol.indexs)

            if(compute.permanence && length(curr.perm.eval.results)>0){
                for(key in names(curr.perm.eval.results)){
                    perm.eval.results[[key]] = c(perm.eval.results[[key]], curr.perm.eval.results[[key]])
                }
            }

            if(length(new.sol.indexs)>0){
                discovered[new.sol.indexs] = TRUE
                next.sol.indexs = new.sol.indexs
            } else {
                break
            }
        }

        edit.comps[[as.character(counter)]] = c(edit.comps[[as.character(counter)]], discovered.ordered.sol.indxs)
    }
    cat("final size: ", length(which(discovered==TRUE)), "/", length(discovered),"\n")

    if(counter>1){
        for(i in counter:2){
            edit.comps[[as.character(i)]] = edit.comps[[as.character(i)]][-which(edit.comps[[as.character(i)]] %in% edit.comps[[as.character(i-1)]])]
        }
    }

    if(compute.permanence){
        for(key in names(perm.eval.results)){
            print(key)
            print(table(perm.eval.results[[key]]))
        }
    }

    results = list()
    results[["nb.enum.jump"]] = counter
    results[["perm.eval.results"]] = perm.eval.results
    results[["edit.comps"]] = edit.comps
    results[["discovered.ordered.sol.indxs"]] = discovered.ordered.sol.indxs

    return(results)
}


##################################################
#
#
##################################################
perform.one.pass = function(g, A, edge.mat, memberships, next.sol.indexs, discovered, max.nb.edit, lib.folder, opt.imb.count, compute.permanence){
    perm.eval.results = list()
    new.sol.indexs = c()
    for(sol.idx in next.sol.indexs){
        for(nb.edit in 1:max.nb.edit){
            accessible.sol.indexs = which(m[sol.idx,]==nb.edit)
            curr.new.sol.indexs = accessible.sol.indexs[which(discovered[accessible.sol.indexs] == FALSE)]
            curr.undecomposable.new.sol.idxs = curr.new.sol.indexs
            if(nb.edit!=1 && length(curr.undecomposable.new.sol.idxs)>0)
                curr.undecomposable.new.sol.idxs = filter.new.sol.indexs.by.decomposable.edit(g, A, edge.mat, memberships, sol.idx, curr.new.sol.indexs, lib.folder, opt.imb.count)
            if(length(curr.undecomposable.new.sol.idxs)>0){
                new.sol.indexs = c(new.sol.indexs, curr.undecomposable.new.sol.idxs)

                discovered[curr.undecomposable.new.sol.idxs] = TRUE # update this for the current method, since the next solutions can find them again

                if(compute.permanence && nb.edit>4){
                    curr.results = evaluate.with.permanence.models(g, A, memberships, sol.idx, curr.undecomposable.new.sol.idxs, lib.folder)
                    #cat("cur sol id: ",sol.idx,", new.sol.idxs: ",curr.undecomposable.new.sol.idxs,"\n")
                    for(key in names(curr.results)){
                        perm.eval.results[[key]] = c(perm.eval.results[[key]], curr.results[[key]])
                    }
                }
            }
            #new.sol.indexs = c(new.sol.indexs, curr.new.sol.indexs)
        }
    }

    results = list()
    results[["perm.eval.results"]] = perm.eval.results
    results[["new.sol.indexs"]] = new.sol.indexs
    return(results)
}


##################################################
#
#
##################################################
evaluate.with.permanence.models = function(g, A, memberships, curr.sol.idx, new.sol.idxs, lib.folder){
    nb.worst.score = 25

    eval.results = list()
    for(new.sol.idx in new.sol.idxs){
        cat("cur sol id: ",curr.sol.idx,", new.sol.idx: ",new.sol.idx,"\n")
        input.membership = memberships[[curr.sol.idx]]
        output.membership = memberships[[new.sol.idx]]
        rel.output.membership = get.relative.membership(input.membership, output.membership, lib.folder)
        moving.nodes = get.moving.nodes(input.membership, rel.output.membership, lib.folder)
        nb.edit = length(moving.nodes)


        is.diff = is.operation.from.diff.clusters(input.membership, moving.nodes)
        is.almost.diff = is.operation.from.almost.diff.clusters(input.membership, moving.nodes)
        is.single.split = is.split.from.single.cluster(input.membership, rel.output.membership, moving.nodes)
        is.almost.single.split = is.almost.split.from.single.cluster(input.membership, rel.output.membership, moving.nodes)
        
        results = compute.permanence.scores(g, A, input.membership)

        cluster.sizes = table(input.membership)
        #nb.node.in.small.clusters = sum(cluster.sizes[which(cluster.sizes<5)])
        sub.cluster.sizes = cluster.sizes[as.character(input.membership[moving.nodes])]
        cat("moving.nodes:",moving.nodes,"(",sub.cluster.sizes,")","\n")

        #if(length(which(sub.cluster.sizes>3))>0){
            sub.moving.nodes = moving.nodes # moving.nodes[which(sub.cluster.sizes>3)]
            print("---")
            for(key in names(results)){
                scores = results[[key]]
                names(scores) = seq(1,length(scores))
                ordered.scores = scores[order(scores)]
                #subset = which(scores<0)
                subset = as.integer(names(ordered.scores[1:nb.worst.score]))
                #subset = sample(seq(1,length(scores)), size=15, replace=F)
                cat("key:",key,", size: ",length(which(scores<0)), ", min()", min(scores), ", max(): ", max(scores),"\n")
                print(scores[sub.moving.nodes])
                eval.value = as.integer(length(which(subset %in% sub.moving.nodes)) == length(sub.moving.nodes))
                eval.results[[key]] = c(eval.results[[key]], eval.value)
            }
        #}

##        if(is.single.split || is.almost.single.split){ # since this situation is hard to predict with permanence scores, I remove this part
##            key = "4"
##            scores = results[[key]]
##            names(scores) = seq(1,length(scores))
##            subset = which(scores>-0.5 & scores<0.5)
##            eval.value = as.integer(length(which(subset %in% moving.nodes)) == nb.edit)
##            #cat("size: ", length(subset), "/", length(scores),"\n")
##            eval.results[[key]] = c(eval.results[[key]], eval.value)
##        }
##
##        if(is.diff || is.almost.diff){ # this is more conveient for permenance scores
##            #for(key in names(results)){
##                key = "3"
##                scores = results[[key]]
##                names(scores) = seq(1,length(scores))
##                ordered.scores = scores[order(scores)]
##                worst.subset = as.integer(names(ordered.scores[1:nb.worst.score]))
##                eval.value = as.integer(length(which(worst.subset %in% moving.nodes)) == nb.edit)
##
##                # another possibility
###               eval.value = as.integer(all(scores[moving.nodes]<0))
##
##                eval.results[[key]] = c(eval.results[[key]], eval.value)
##            #}
##        }
    }
    return(eval.results)
}


# ==============================================================
# ==============================================================


MBRSHP.FILE.PREFIX = "membership"

lib.folder = "."
network.path = "net.G"
g = read.graph.ils(network.path)
A = as_adjacency_matrix(g, type = c("both"), attr="weight")
A = as.matrix(A)

edge.mat <- get.edgelist(g)
edge.mat <- matrix(as.integer(edge.mat), nrow(edge.mat), ncol(edge.mat))
if(edge.mat[1,1] == 0) # check if node ids start from 0 or 1. This affects directly 'clus.mat' variable
	edge.mat = edge.mat + 1

max.nb.edit = 4
m = read.csv("dist-matrix-Edit.csv", row.names=1)

folder = "."
memberships = load.membership.files(folder)

opt.imb.count = compute.imbalance.from.membership(g, edge.mat, memberships[[1]], output.type = "count")

start=Sys.time()
compute.permanence = FALSE
start.sol.indx = 1
results = perform.my.enum.algo(g, A, edge.mat, memberships, m, max.nb.edit, lib.folder, opt.imb.count, start.sol.indx, compute.permanence)
perm.eval.results = results[["perm.eval.results"]]
nb.enum.jump = results[["nb.enum.jump"]]
edit.comps = results[["edit.comps"]]
discovered.ordered.sol.indxs = results[["discovered.ordered.sol.indxs"]]
end=Sys.time()
exec.time = as.numeric(end) - as.numeric(start)
cat("exec time: ", exec.time, "s \n")
cat("nb.enum.jump: ", nb.enum.jump, "\n")
print(edit.comps)
cat("ordered sol indexes: ", discovered.ordered.sol.indxs, "\n")

if(nb.enum.jump>1){
    for(i in 1:(nb.enum.jump-1)){
        for(j in (i+1):nb.enum.jump){
            subm = as.matrix(m[edit.comps[[as.character(i)]], edit.comps[[as.character(j)]]], nrow=length(edit.comps[[as.character(i)]]), ncol=length(edit.comps[[as.character(j)]]))
            indxs = which(subm == min(subm), arr.ind = TRUE)[1,]
            row.indx = indxs[1]
            col.indx = indxs[2]
            # note that sol.idx1 is enumerted before sol.idx2, since it belongs to i.th edit component
            sol.idx1 = edit.comps[[as.character(i)]][row.indx]
            sol.idx2 = edit.comps[[as.character(j)]][col.indx]
            
            print("--")
            cat("nb.edit: ", min(subm)," >> i: ",i,", j: ",j, " => (",sol.idx1,",",sol.idx2,")","\n")

            res = evaluate.with.permanence.models(g, A, memberships, sol.idx1, sol.idx2, lib.folder)
            print(res)
        }
    }
}
