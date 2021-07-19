This package contains the test runners for testing the -Ainfer command-line
argument.
They are in a separate package so that they don't run by default; they should
only run when they're invoked directly by their corresponding build rules, which
are in checker/build.gradle (ainferTest).
