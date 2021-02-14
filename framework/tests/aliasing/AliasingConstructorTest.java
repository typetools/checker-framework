import org.checkerframework.common.aliasing.qual.*;

public class AliasingConstructorTest {

    public AliasingConstructorTest(@NonLeaked Object o) {}

    // int and String parameters on the constructors below are used only
    // to make a distinction among constructors.
    public AliasingConstructorTest(@LeakedToResult Object o, int i) {}

    public AliasingConstructorTest(Object o, String s) {}

    public void annosInAliasingConstructorTest() {
        @Unique Object o = new Object();
        new AliasingConstructorTest(o);
        Object o2 = new Object();
        new AliasingConstructorTest(o2, 1);
        AliasingConstructorTest ct = new AliasingConstructorTest(o2, 1);
        @Unique Object o3 = new Object();
        // ::error: (unique.leaked)
        new AliasingConstructorTest(o3, "someString");
    }
}
