

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
compute.imbalance.from.membership = function(g, membership, output.type = "count"){

	membership = as.integer(membership)
	edge.mat <- get.edgelist(g)
	edge.mat <- matrix(as.integer(edge.mat), nrow(edge.mat), ncol(edge.mat))
	if(edge.mat[1,1] == 0) # check if node ids start from 0 or 1. This affects directly 'clus.mat' variable
		edge.mat = edge.mat + 1
	
	clus.mat <- cbind(membership[edge.mat[,1]], membership[edge.mat[,2]])
		
	neg.links <- E(g)$weight<0
	pos.links <- E(g)$weight>=0

	misplaced <- (clus.mat[,1]==clus.mat[,2] & neg.links) | (clus.mat[,1]!=clus.mat[,2] & pos.links)
	imb.val = sum(abs(E(g)$weight[misplaced]))
	
	# --------------------------------
	lpos.imbalance <- E(g)$weight * as.numeric(misplaced & pos.links)
	lneg.imbalance <- abs(E(g)$weight) * as.numeric(misplaced & neg.links)
	npos.imbalance <- sapply(1:vcount(g), function(u) 
			{	idx <- which(edge.mat[,1]==u | edge.mat[,2]==u)
				result <- sum(lpos.imbalance[idx])
				return(result)
			})
	nneg.imbalance <- sapply(1:vcount(g), function(u) 
			{	idx <- which(edge.mat[,1]==u | edge.mat[,2]==u)
				result <- sum(lneg.imbalance[idx])
				return(result)
			})
	
	max.val = max(c(npos.imbalance,nneg.imbalance))
	if(max.val != 0){ # if the situation has some imbalance
		npos.imbalance <- npos.imbalance / max.val # normalized
		nneg.imbalance <- nneg.imbalance / max.val # normalized
	}
	# --------------------------------
	
	# make them explicit
	n.in.clu.imb = nneg.imbalance # negative misplaced links are the misplaced link insde clusters
	n.betw.clu.imb = npos.imbalance # pisitive misplaced links are the misplaced link between clusters
	
	# ========================
	# ========================
	
	if(output.type == "count")
		return(format(round(imb.val, 3), nsmall = 3)) # 3 decimal floating
	else if(output.type == "percentage"){
		perc = (imb.val/ sum(abs(E(g)$weight)))*100
		return(format(round(perc, 3), nsmall = 3))
	} else if(output.type == "node.imbalance") # normalized
		return( list(in.imb=n.in.clu.imb, betw.imb=n.betw.clu.imb) )
	else
		return(NA)
	
}


##################################################
#
#
##################################################
is.subedit.operation = function(g, A, input.membership, output.membership, lib.folder, opt.imb.count){
    rel.output.membership = get.relative.membership(input.membership, output.membership, lib.folder)
    moving.nodes = get.moving.nodes(input.membership, output.membership, lib.folder)
    nb.edit = length(moving.nodes)

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
        for(nb.subedit in 1:floor(nb.edit/2)){
            combs = combn(moving.nodes, nb.subedit)
            for(i in 1:ncol(combs)){
                sub.moving.nodes = combs[,i]
                temp.output.membership = input.membership
                temp.output.membership[sub.moving.nodes] = rel.output.membership[sub.moving.nodes]
                imb.count = compute.imbalance.from.membership(g, temp.output.membership, output.type = "count")
                if(imb.count == opt.imb.count)
                    return(TRUE)
            }
        }
    }
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
	rel.mem2 = get.relative.membership(mem1, mem2, lib.folder)

	moving.nodes = c()
	for(i in 1:n){
		if(mem1[i] != rel.mem2[i])
			moving.nodes = c(moving.nodes, i) # node id and index is different
	}

	return(moving.nodes)
}


##################################################
#
#
##################################################
filter.new.sol.indexs.by.decomposable.edit = function(g, A, memberships, curr.sol.idx, new.sol.idxs, lib.folder, opt.imb.count){
    undecomposable.new.sol.idxs = c()
    for(new.sol.idx in new.sol.idxs){
        input.membership = memberships[[curr.sol.idx]]
        output.membership = memberships[[new.sol.idx]]
        res = is.subedit.operation(g, A, input.membership, output.membership, lib.folder, opt.imb.count)
        if(!res)
            undecomposable.new.sol.idxs = c(undecomposable.new.sol.idxs, new.sol.idx)
    }
    return(undecomposable.new.sol.idxs)
}


##################################################
#
#
##################################################
# m is the edit dist matrix
find.nb.enum.jump = function(g, A, memberships, m, max.nb.edit, lib.folder, opt.imb.count){
    discovered = rep(FALSE,nrow(m))
    counter = 0
    while(any(discovered == FALSE)){    
        counter = counter + 1
        cat("counter: ", counter, " => current size: ", length(which(discovered==TRUE)),"\n")
        next.sol.indexs = which(discovered == FALSE)[1]
        discovered[next.sol.indexs] = TRUE

        while(TRUE){
            new.sol.indexs = perform.one.pass(g, A, memberships, next.sol.indexs, discovered, max.nb.edit, lib.folder, opt.imb.count)
            if(length(new.sol.indexs)>0){
                discovered[new.sol.indexs] = TRUE
                next.sol.indexs = new.sol.indexs
            } else {
                break
            }
        }
    }
    cat("final size: ", length(which(discovered==TRUE)), "/", length(discovered),"\n")
    return(counter)
}


##################################################
#
#
##################################################
perform.one.pass = function(g, A, memberships, next.sol.indexs, discovered, max.nb.edit, lib.folder, opt.imb.count){
    new.sol.indexs = c()
    for(sol.idx in next.sol.indexs){
        for(nb.edit in 1:max.nb.edit){
            accessible.sol.indexs = which(m[sol.idx,]==nb.edit)
            curr.new.sol.indexs = accessible.sol.indexs[which(discovered[accessible.sol.indexs] == FALSE)]
            curr.undecomposable.new.sol.idxs = curr.new.sol.indexs
            if(nb.edit!=1 && length(curr.undecomposable.new.sol.idxs)>0)
                curr.undecomposable.new.sol.idxs = filter.new.sol.indexs.by.decomposable.edit(g, A, memberships, sol.idx, curr.new.sol.indexs, lib.folder, opt.imb.count)
            if(length(curr.undecomposable.new.sol.idxs)>0)
                new.sol.indexs = c(new.sol.indexs, curr.undecomposable.new.sol.idxs)
            #new.sol.indexs = c(new.sol.indexs, curr.new.sol.indexs)
        }
    }
    return(new.sol.indexs)
}


# ==============================================================
# ==============================================================

MBRSHP.FILE.PREFIX = "membership"

lib.folder = "."
network.path = "net.G"
g = read.graph.ils(network.path)
A = as_adjacency_matrix(g, type = c("both"), attr="weight")
A = as.matrix(A)

max.nb.edit = 5
m = read.csv("dist-matrix-Edit.csv", row.names=1)

folder = "."
memberships = load.membership.files(folder)

opt.imb.count = compute.imbalance.from.membership(g, memberships[[1]], output.type = "count")

start=Sys.time()
nb.enum.jump = find.nb.enum.jump(g, A, memberships, m, max.nb.edit, lib.folder, opt.imb.count)
end=Sys.time()
exec.time = as.numeric(end) - as.numeric(start)
cat("exec time: ", exec.time, "s \n")
print(nb.enum.jump)
