# Put user-specific changes in your own Makefile.user.
# Make will silently continue if that file does not exist.
-include Makefile.user

CHECKER_QUAL_JAR=../../checker/dist/checker-qual.jar

ZIPFILES = sourcefiles.zip

all: ${ZIPFILES}

sourcefiles.zip: clean-classes
	rm -f src/README.txt
	cp -pf README-sourcefiles.txt src/README.txt
	zip -r sourcefiles.zip src/

clean-classes:
	find . -name '*.class' ! -name '.*' -exec rm {} +

clean: clean-classes
	@\rm -f ${ZIPFILES}
