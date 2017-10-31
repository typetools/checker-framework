.PHONY: all

all: clean
	mvn -fn compile > Out.txt 2>&1
	grep -qF "MavenExample.java:[25,30] [assignment.type.incompatible] incompatible types in assignment." Out.txt

clean:
	mvn -q clean
	rm -f Out.txt
