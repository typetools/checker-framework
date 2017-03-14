import org.checkerframework.checker.index.qual.*;

public class SameLenTripleThreat {
    public void foo(String[] vars) {
        String[] qrets = new String[vars.length];
        String @SameLen("vars") [] y = qrets;
        String[] indices = new String[vars.length];
        String @SameLen("qrets") [] x = indices;
    }
}
