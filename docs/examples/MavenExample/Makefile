JAVA_VER := $(shell java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1 | sed 's/-ea//')

.PHONY: all test clean

all:
# `-fn` command-line argument means never fail the build.
# The `mvn` command is supposed to fail, but sometimes it fails due to trouble
# downloading artifacts, so retry just in case.
# `-U` makes Maven request again an artifact whose download failed in an earlier
# run.  Without it, Maven records the failure in its local repository and does not
# request the artifact again until the repository's update interval (by default, a
# day) has elapsed, so a retry fails the same way.
	mvn -B -U clean > /dev/null 2>&1 || mvn -B -U clean
	mvn -B -U -fn compile > Out.txt 2>&1
	sleep 5
	mvn -B -U clean > /dev/null 2>&1
	mvn -B -U -fn compile > Out.txt 2>&1
	(grep -qF "MavenExample.java:[29,25] error: [assignment] incompatible types in assignment." Out.txt || (echo "FAILURE.  Here is file Out.txt:" && cat Out.txt && echo "End of file Out.txt." && false))

test: all

clean:
	@mvn -q clean
	@rm -f Out.txt
