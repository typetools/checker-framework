/*
 * @test
 * @summary Test that unchecked warnings are issued, without and with processor
 *
 * @compile/ref=UncheckedWarning.out -XDrawDiagnostics -Xlint:unchecked UncheckedWarning.java
 * @compile/ref=UncheckedWarning.out -XDrawDiagnostics -Xlint:unchecked -processor org.checkerframework.checker.nullness.NullnessChecker -Alint UncheckedWarning.java
 */

import java.util.ArrayList;
import java.util.List;

class Test<T> {
    List<T> foo() {
        List<String> ret = new ArrayList<String>();
        ret.add("Hi there!");
        return (List) ret;
    }
}

public class UncheckedWarning {
    public static void main(String[] args) {
        Test<Integer> ti = new Test<Integer>();
        List<Integer> ls = ti.foo();
        Integer i = ls.get(0);
    }
}
