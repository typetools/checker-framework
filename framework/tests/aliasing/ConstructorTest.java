import org.checkerframework.common.aliasing.qual.*;

public class ConstructorTest {

    public ConstructorTest(@NonLeaked Object o) {}

    // int and String parameters on the constructors below are used only
    // to make a distinction among constructors.
    public ConstructorTest(@LeakedToResult Object o, int i) {}

    public ConstructorTest(Object o, String s) {}

    public void annosInConstructorTest() {
        @Unique Object o = new Object();
        new ConstructorTest(o);
        Object o2 = new Object();
        new ConstructorTest(o2, 1);
        ConstructorTest ct = new ConstructorTest(o2, 1);
        @Unique Object o3 = new Object();
        // ::error: (unique.leaked)
        new ConstructorTest(o3, "someString");
    }
}
