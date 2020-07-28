g = read.graph.ils("../../in/net.G")

source("~/eclipse/workspace-neon/Sosocc/src/evaluate-partitions/evaluate-imbalance.R")
for(i in 0:21){mem = read.table(paste0("membership",i,".txt"))$V1; print(compute.imbalance.from.membership(g,mem))}

for(i in 0:21){ for(j in 0:21){ if(i!=j){ mem1=read.table(paste0("membership",i,".txt"))$V1; mem2=read.table(paste0("membership",j,".txt"))$V1; print(compare(mem1, mem2, "rand"))  }}}

