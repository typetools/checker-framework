#!/bin/sh

# TODO: Remove all dependencies on /homes/gws
export PERL5LIB=/homes/gws/mernst/bin/src/perl:/homes/gws/mernst/bin/src/perl/share/perl5:/homes/gws/mernst/bin/src/perl/lib64/perl5:/usr/share/perl5/

# Argument #1 is extra command-line arguments.
# Argument #2 is URL to check

# shellcheck disable=SC2086
"${CHECKLINK}/checklink" -q -r -e "$(grep -v '^#' "${CHECKLINK}/checklink-args.txt")" $1 "$2"
