import org.checkerframework.checker.signature.qual.*;

// Not on classpath when running the Checker Framework tests.
// import org.apache.bcel.generic.ClassGen;

public class StubLibraryTest {

  void testJdk() {
    @ClassGetName String s3 = String.class.getName();
  }

  //   void testBcel(ClassGen cg) {
  //     @ClassGetName String cgn = cg.getClassName();
  //     @BinaryName String bn = cg.getClassName();
  //   }

}
