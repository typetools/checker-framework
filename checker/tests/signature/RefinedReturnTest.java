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
        public @IdentifierOrArray String aString() {
            return "Integer[]";
        }
    }

    void m() {
        @IdentifierOrArray String s = new Sub().aString();
    }
}
