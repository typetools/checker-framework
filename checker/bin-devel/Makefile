remove-annotations.sh:
	\rm -f remove-annotations.sh
	echo "# Remove Checker Framework annotations (except trusted ones) from all files under the current directory." >> remove-annotations.sh
	echo "" >> remove-annotations.sh
# TODO: only remove imports for annotations that are removed.  (e.g., do not remove InternedDistinct import)
	echo "preplace '^import org.checkerframework.*\\\n' ''" >> remove-annotations.sh
# Declaration annotations that appear on their own line.
	echo "preplace '^ *\@(Deterministic|EnsuresNonNull|FormatMethod|Pure|RequiresNonNull|SideEffectFree)(\([^()]*\))?\\\n' ''" >> remove-annotations.sh
# The `grep -v` removes trusted annotations.
# TODO: Maybe have two variants of the script, with and without trusted annotations.
# TODO: Don't remove annotations that are within the scope of @SuppressWarnings.  (That requires a real parser rather than this script that uses regexes.)
	grep '^    "[^"]*",' .run-google-java-format/fixup-google-java-format.py | grep -v InternedDistinct | sed 's/^    "\(.*\)",/preplace '"'"'\\@\1(\\([^()"]*("[^"]*"[^()"]*)*\\))? '"'"' '"''"'/' >> remove-annotations.sh
	echo "preplace '\([ \\\n]*[A-Za-z][A-Za-z0-9_]*(<[^<>]*>)? this\)' '()'" >> remove-annotations.sh
	echo "preplace '\([ \\\n]*[A-Za-z][A-Za-z0-9_]*(<[^<>]*>)? this,[ \\\t]*' '('" >> remove-annotations.sh
	chmod +x remove-annotations.sh
