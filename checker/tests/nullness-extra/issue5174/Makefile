.PHONY: all

all: clean
	$(JAVAC) -processor org.checkerframework.checker.nullness.NullnessChecker -Acfgviz=org.checkerframework.dataflow.cfg.visualize.StringCFGVisualizer -AassumeKeyFor Issue5174.java > Out.txt 2>&1 || true
	diff -u Issue5174.out Out.txt

clean:
	rm -f *.class Out.txt
