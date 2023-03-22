#!/bin/sh

# TODO: Remove all dependencies on /homes/gws

export PERL5LIB=/usr/local/lib64/perl5/5.32:/usr/local/share/perl5/5.32:/usr/lib64/perl5/vendor_perl:/usr/share/perl5/vendor_perl:/usr/lib64/perl5:/usr/share/perl5


# Argument #1 is extra command-line arguments.
# Argument #2 is URL to check

# shellcheck disable=SC2046 disable=SC2086
perl -wT "${CHECKLINK}"/checklink -q -r -e $(grep -v '^#' "${CHECKLINK}"/checklink-args.txt) $1 $2
