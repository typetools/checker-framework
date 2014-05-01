# Put user-specific changes in your own Makefile.user.
# Make will silently continue if that file does not exist.
-include Makefile.user

CHECKER_QUAL_JAR=../checker/dist/checker-qual.jar

all: eclipse-projects.zip sourcefiles.zip 

eclipse-projects.zip: copy-checker-qual clean-classes
	zip -r eclipse-projects.zip eclipse-projects/

copy-checker-qual:
	mkdir -p eclipse-projects/checker-framework-tutorial/lib
	mkdir -p eclipse-projects/encryption-checker/lib
	cp ${CHECKER_QUAL_JAR} eclipse-projects/checker-framework-tutorial/lib/
	cp ${CHECKER_QUAL_JAR} eclipse-projects/encryption-checker/lib/
	cp ${CHECKER_QUAL_JAR} eclipse-projects/personalblog-demo/lib/

sourcefiles.zip: copy-checker-qual clean-classes copy-files-from-eclipse
	zip -r sourcefiles.zip src/

copy-files-from-eclipse:
	mkdir -p src/myqual
	mkdir -p src/encrypted 
	cp eclipse-projects/checker-framework-tutorial/src/*.java src
	cp eclipse-projects/encryption-checker/src/myqual/*.java src/myqual/
	cp eclipse-projects/encryption-checker/src/encrypted/*.java src/encrypted/
	cp -r eclipse-projects/personalblog-demo src

clean-classes: 
	find . -name '*.class' ! -name '.*' -exec rm {} +

clean: clean-classes
	@\rm -f  eclipse-projects.zip sourcefiles.zip 
	rm -rf   eclipse-projects/checker-framework-tutorial/lib/\
                 eclipse-projects/encryption-checker/lib/\
                 src/\
                 eclipse-projects/personalblog-demo/lib/checker-qual.jar
