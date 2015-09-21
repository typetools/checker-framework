import org.checkerframework.framework.qual.AnnotatedFor;

// Tests that the -AsafeDefaultsForUnannotatedBytecode option does not
// affect defaulting in source code.

class BasicTest {
  void f() {
    g("");
  }

  void g(String s) {}
}
