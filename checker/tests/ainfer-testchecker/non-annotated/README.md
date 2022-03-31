The files in this directory are tested while using the -Ainfer option.
For this reason, each of these files is typechecked twice: once to infer
annotations (the "Generate" phase), and a second time
to ensure no more errors are issued once whole-program inference has been
run (the "Validate" phase). Between these two phases, the files
are copied to the ../annotated/ directory (i.e., the files in that
directory are the inputs to the "Validate" phase) and all expected errors and
warnings are removed. The ../inference-output/ contains the produced
annotation files (i.e., the .jaif, .astub, or .ajava files).

Because all the annotation files are stored in the same directory, there
are two requirements for test inputs that are typechecked as part of
these tests (that is, the test files in this directory and all all-systems
tests):
* the fully-qualified name of each test class must be unique, because annotation
  files use a naming scheme based on the fully-qualified name of the class. This
  means, for example, that this directory cannot contain any test files with the
  same name as any all-systems test.
* every class must either be or be contained by a class whose name matches the name
  of the file containing the class. This requirement is caused by the need to locate
  programmatically the annotation files generated from a particular test.
