# Put user-specific changes in your own Makefile.user.
# Make will silently continue if that file does not exist.
-include Makefile.user

CHECKERS_QUALS_JAR=../checkers/binary/checkers-quals.jar

all: eclipse-projects.zip sourcefiles.zip 

eclipse-projects.zip: copy-checkers-quals clean-classes
	zip -r eclipse-projects.zip eclipse-projects/

copy-checkers-quals:
	cp ${CHECKERS_QUALS_JAR} eclipse-projects/checker-framework-tutorial/lib
	cp ${CHECKERS_QUALS_JAR} eclipse-projects/encryption-checker/lib
	cp ${CHECKERS_QUALS_JAR} eclipse-projects/personalblog-demo/lib

sourcefiles.zip: clean-classes copy-files-from-eclipse
	zip -r sourcefiles.zip src/

copy-files-from-eclipse:
	cp eclipse-projects/checker-framework-tutorial/src/*.java src
	cp eclipse-projects/encryption-checker/src/myquals/*.java src/myquals/
	cp eclipse-projects/encryption-checker/src/encrypted/*.java src/encrypted/
	cp -r eclipse-projects/personalblog-demo src

clean-classes: 
	find . -name '*.class' ! -name '.*' -exec rm {} +


clean: clean-classes
	@\rm -f  eclipse-projects.zip sourcefiles.zip 
	rm -f    eclipse-projects/checker-framework-tutorial/lib/checkers-quals.jar\
                 eclipse-projects/encryption-checker/lib/checkers-quals.jar\
                 eclipse-projects/personalblog-demo/lib/checkers-quals.jar


