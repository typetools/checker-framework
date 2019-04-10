import testlib.util.*;

// ::warning: (inconsistent.constructor.type) :: error: (super.invocation.invalid)
public @Odd class ClassAnnotations {

    ClassAnnotations c;

    public void test() {
        @Odd ClassAnnotations d = c;
    }
}
