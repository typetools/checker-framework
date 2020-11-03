remove-annotations.sh:
	\rm -f remove-annotations.sh
	echo "# Remove Checker Framework annotations from all files under the current directory." >> remove-annotations.sh
	echo "" >> remove-annotations.sh
	echo "preplace '^import org.checkerframework.*\\\n' ''" >> remove-annotations.sh
	echo "preplace '^ *\@(Deterministic|EnsuresNonNull|FormatMethod|Pure|SideEffectFree)(\([^()]*\))?\\\n' ''" >> remove-annotations.sh
	grep '^    "[^"]*",' .run-google-java-format/fixup-google-java-format.py | sed 's/^    "\(.*\)",/preplace '"'"'\\@\1(\\([^()"]*("[^"]*"[^()"]*)*\\))? '"'"' '"''"'/' >> remove-annotations.sh
	echo "preplace '\([ \\\n]*[A-Za-z][A-Za-z0-9_]*(<[^<>]*>)? this\)' '()'" >> remove-annotations.sh
	echo "preplace '\([ \\\n]*[A-Za-z][A-Za-z0-9_]*(<[^<>]*>)? this,[ \\\t]*' '('" >> remove-annotations.sh
	chmod +x remove-annotations.sh
