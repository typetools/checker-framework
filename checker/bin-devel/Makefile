ALL_FILES=Dockerfile-ubuntu-jdkbase Dockerfile-ubunturolling-jdkbase \
 Dockerfile-ubuntu-jdkplus Dockerfile-ubunturolling-jdkplus \
 Dockerfile-ubuntu-jdk17 Dockerfile-ubuntu-jdk17-plus \
 Dockerfile-ubuntu-jdk21 Dockerfile-ubuntu-jdk21-plus \
 Dockerfile-ubuntu-jdk25 Dockerfile-ubuntu-jdk25-plus \
 Dockerfile-ubuntu-jdk26 Dockerfile-ubuntu-jdk26-plus

.PHONY: all test clean default
default: all
all: ${ALL_FILES}

test: all

clean:
	rm -f ${ALL_FILES}

%: %.m4 Dockerfile-contents-ubuntu-base.m4 Dockerfile-contents-ubuntu-plus.m4 Dockerfile-contents-apt-clean.m4
	@chmod -f +w $@ || true
	m4 $< > $@.tmp
# Remove trailing blank lines.  Use `awk` because `sed`'s branch syntax is not portable.
	@awk 'NF {last=NR} {line[NR]=$$0} END {for (i = 1; i <= last; i++) print line[i]}' $@.tmp > $@
	@rm -f $@.tmp
	@chmod -w $@
