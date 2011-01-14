This is the checker-framework repository.

This repository contains several related projects:

 checkers    the Checker Framework proper
 demos       slides and source code for demos of the Checker Framework
 javaparser  a parser for Java source code; supports type annotations
 release     buildfiles for making a relase


To build the Checker Framework, run:
  cd checkers
  ant

To generate the Checker Framework manual, run:
  make -C checkers/manual
This produces the two files:
  checkers/manual/manual.html
  checkers/manual/manual.pdf
Alternately, you can find the manual on the Web at
  http://types.cs.washington.edu/checker-framework/
