JAVA_VER := $(shell java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)

.PHONY: all clean

all:
# `-fn` command-line argument means never fail the build.
# The `mvn` command is supposed to fail, but sometimes it fails due to trouble
# downloading artifacts, so retry just in case.
	mvn -B clean > /dev/null 2>&1
	mvn -B -fn compile > Out.txt 2>&1
	sleep 5s
	mvn -B clean > /dev/null 2>&1
	mvn -B -fn compile > Out.txt 2>&1
# Temporary hack to skip running under JDK > 11.  This test gets a hard-coded version of the Checker Framework from Maven Central, and that version doesn't work on JDK 16 (until the next release after this line is committed!).
	[ ${JAVA_VER} -gt 11 ] || \
	(grep -qF "MavenExample.java:[29,29] error: [assignment.type.incompatible] incompatible types in assignment." Out.txt || (echo "FAILURE.  Here is file Out.txt:" && cat Out.txt && echo "End of file Out.txt." && false))

clean:
	mvn -q clean
	rm -f Out.txt
