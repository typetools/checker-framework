.PHONY: all

# Delomboking seems to mess up line numbers. The actual error is on line 13, but the error appears on line 12.
# So check for both the error message and make sure it is for the right assignment.
all: clean
	- ../../../gradlew build > Out.txt 2>&1
	grep -qF "User.java:9: error: [argument.type.incompatible] incompatible types in argument." Out.txt && \
		grep -qF "Foo.java:12: error: [assignment.type.incompatible] incompatible types in assignment." Out.txt && \
		grep -qF "y = null; // error" Out.txt

clean:
	../../../gradlew clean
	rm -f Out.txt
