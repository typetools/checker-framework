#!/bin/sh

export PLUME_BIN=/homes/gws/mernst/bin/share-plume/
export PERL5LIB=/homes/gws/mernst/bin/src/perl:/homes/gws/mernst/bin/src/perl/share/perl5:/homes/gws/mernst/bin/src/perl/lib64/perl5:/homes/gws/mernst/research/steering/colony-2003/experiment-scripts:/usr/share/perl5/:/homes/gws/jburke/perl_lib 

${PLUME_BIN}/checklink -q -r -e `grep -v '^#' ${PLUME_BIN}/checklink-args.txt` $1
