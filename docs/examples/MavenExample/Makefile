.PHONY: all

all: clean
	(mvn -B -fn compile > Out.txt 2>&1) \
	  || (sleep 5m && mvn -B -fn compile > Out.txt 2>&1)
	grep -qF "MavenExample.java:[29,29] error: [assignment.type.incompatible] incompatible types in assignment." Out.txt || (cat Out.txt && false)

clean:
	mvn -q clean
	rm -f Out.txt
