.PHONY: all test clean

all: clean
	-$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker -Acfgviz=org.checkerframework.dataflow.cfg.visualize.StringCFGVisualizer -AassumeKeyFor Issue5174.java > Out.txt 2>&1
	diff -u Issue5174.goal Out.txt

test: all

clean:
	rm -f *.class Out.txt
