import org.checkerframework.framework.qual.AnnotatedFor;

// Tests that the -AsafeDefaultsForUnannotatedBytecode option does not
// affect defaulting nor suppress errors in source code.

class BasicTest {
  void f() {
    g("");
  }

  void g(String s) {}
}

@AnnotatedFor("nullness")
class HasErrors {
  Object f() {
    //:: error: (return.type.incompatible)
    return null;
  }
}

class HasErrors2 {
  Object f() {
    //:: error: (return.type.incompatible)
    return null;
  }
}
