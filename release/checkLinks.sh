#!/bin/sh

# export PLUME_BIN=/scratch/${USER}/jsr308-release/build/plume-lib/bin
# TODO: Remove all dependencies on /homes/gws
export PERL5LIB=/homes/gws/mernst/bin/src/perl:/homes/gws/mernst/bin/src/perl/share/perl5:/homes/gws/mernst/bin/src/perl/lib64/perl5:/usr/share/perl5/:/homes/gws/jburke/perl_lib

${PLUME_BIN}/checklink -q -r -e `grep -v '^#' ${PLUME_BIN}/checklink-args.txt` $1 $2
