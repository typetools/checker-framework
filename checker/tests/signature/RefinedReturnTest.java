import org.checkerframework.checker.signature.qual.*;

// Not on classpath when running the Checker Framework tests.
// import org.apache.bcel.generic.ClassGen;

public class RefinedReturnTest {

    public class Super {
        public @FullyQualifiedName String aString() {
            return "java.lang.Integer[][]";
        }
    }

    public class Sub extends Super {
        @Override
        public @ArrayWithoutPackage String aString() {
            return "Integer[]";
        }
    }

    void m() {
        @ArrayWithoutPackage String s = new Sub().aString();
    }
}
