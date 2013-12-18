import checkers.signature.quals.*;

// Not on classpath when running the Checker Framework tests.
// import org.apache.bcel.generic.ClassGen;

public class RefinedReturnTest {

  public class Super {
    public @BinaryName String aString() { return "int[][]"; }
  }

  public class Sub extends Super {
    @Override
    public @BinaryNameForNonArray String aString() { return "java.lang.Integer"; }
  }

  void m() {
    @BinaryNameForNonArray String s = new Sub().aString();
  }

}
