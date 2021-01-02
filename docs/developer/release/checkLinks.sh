#!/bin/sh

# TODO: Remove all dependencies on /homes/gws
export PERL5LIB=/homes/gws/mernst/bin/install/ActivePerl-5.28/bin:/homes/gws/mernst/bin/install/ActivePerl-5.28/share/perl5:/homes/gws/mernst/bin/install/ActivePerl-5.28/lib64/perl5:

export PATH=/homes/gws/mernst/bin/install/ActivePerl-5.28/bin:${PATH}

# Argument #1 is extra command-line arguments.
# Argument #2 is URL to check

# shellcheck disable=SC2086
${CHECKLINK}/checklink -q -r -e `grep -v '^#' ${CHECKLINK}/checklink-args.txt` $1 $2
