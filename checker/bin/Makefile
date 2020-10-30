remove-annotations.sh:
	echo "preplace '^import org.checkerframework.*\\\n' ''" > remove-annotations.sh
	echo "preplace '^ *\@(Deterministic|EnsuresNonNull|FormatMethod|Pure|SideEffectFree)(\([^()]*\))?\\\n' ''" >> remove-annotations.sh
	grep '^    "[^"]*",' ../bin-devel/.run-google-java-format/fixup-google-java-format.py | sed 's/^    "\(.*\)",/preplace '"'"'\\@\1(\\([^()"]("[^"]*"[^()"]*)*\\))? '"'"' '"''"'/' >> remove-annotations.sh
	chmod +x remove-annotations.sh
