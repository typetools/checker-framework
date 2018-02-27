# Put user-specific changes in your own Makefile.user.
# Make will silently continue if that file does not exist.
-include Makefile.user

CHECKER_QUAL_JAR=../../checker/dist/checker-qual.jar

ZIPFILES = sourcefiles.zip

all: ${ZIPFILES}

sourcefiles.zip: copy-checker-qual clean-classes copy-files-from-eclipse
	rm -f src/README.txt
	cp -pf README-sourcefiles.txt src/README.txt
	zip -r sourcefiles.zip src/

clean-sourcefiles:
	@\rm -rf src/

clean-classes:
	find . -name '*.class' ! -name '.*' -exec rm {} +

clean: clean-classes clean-checker-qual clean-sourcefiles
	@\rm -f ${ZIPFILES}
