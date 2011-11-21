import checkers.igj.quals.*;

/**
 *
 */
@Immutable
class ManifestClass {
    int i = 4;

    public int getValue() { return 0; }
    public ManifestClass getThis() { return this; }

    public void mutate1() {
        i++;    // should emit error
    }

    // method invocation tests
    public static void process(ManifestClass c) { }
    public void method1() { }
    public void method2() { method1(); }
}

