# This file identifies the root of the test-suite hierarchy.
# It also contains test-suite configuration information.

# The list of keywords supported in the entire test suite
keys=nullness framework stubs

# The tests under slowtypechecking/ measure wall-clock type-checking time, so they must not share
# a JVM with other tests: a JVM warmed up (and its heap filled) by previous compilations reports
# substantially different times for the same file.
othervm.dirs=slowtypechecking
