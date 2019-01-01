.PHONY: all

all: clean
	- ../../../gradlew build > Out.txt 2>&1
	grep -qF "User.java:9: error: [argument.type.incompatible] incompatible types in argument." Out.txt && grep -qF "Foo.java:13: error: [assignment.type.incompatible] incompatible types in assignment." Out.txt

clean:
	../../../gradlew clean
	rm -f Out.txt
