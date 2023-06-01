JAVA_VER := $(shell java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1 | sed 's/-ea//')

.PHONY: all

# Delomboking seems to mess up line numbers. The actual error is on line 13, but the error appears on line 12.
# So check for both the error message and make sure it is for the right assignment.
all: clean
	- ../../../gradlew build > Out.txt 2>&1
	(grep -qF "User.java:9: error: [argument] incompatible argument for parameter y of FooBuilder.y." Out.txt \
	  && grep -qF "Foo.java:12: error: [assignment] incompatible types in assignment." Out.txt \
	  && grep -qF "y = null; // error" Out.txt) \
	 || (echo "===== start of Out.txt =====" && cat Out.txt && echo "===== end of Out.txt =====" && false)

clean:
	../../../gradlew clean
	rm -f Out.txt
