public class JavaErrorTest {
  // Checking that if one checker finds a Java error
  // and therefor does not set a root, then
  // checkers relying on that checker should also not run.
  // otherwise, it might access a subchecker whose "root" has not been set.
  // (See AnnotatedTypeFactory.setRoot(CompilationUnitTree) )
  void foo() {
    Object a;
    // :: error: variable a is already defined in method foo()
    Object a;
  }
}
