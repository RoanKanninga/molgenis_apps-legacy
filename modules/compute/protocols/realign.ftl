#
# =====================================================
# $Id$
# $URL$
# $LastChangedDate$
# $LastChangedRevision$
# $LastChangedBy$
# =====================================================
#

<#include "macros.ftl"/>
<@begin/>
#MOLGENIS walltime=35:59:00 mem=10 cores=1


inputs "${dedupbam}" 
inputs "${indexfile}" 
inputs "${dbsnprod}"
inputs "${pilot1KgVcf}"
inputs "${realignTargets}"
outputs "${realignedbam}"

java -Djava.io.tmpdir=${tempdir} -Xmx10g -jar \
${genomeAnalysisTKjar} \
-l INFO \
-T IndelRealigner \
-U ALLOW_UNINDEXED_BAM \
-I ${dedupbam} \
--out ${realignedbam} \
-targetIntervals ${realignTargets} \
-R ${indexfile} \
-D ${dbsnprod} \
-B:indels,VCF ${pilot1KgVcf} \
-knownsOnly \
-LOD 0.4 \
-maxReads 2000000
<@end />