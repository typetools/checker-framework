import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

class StaticInitializer {

    public static String a;
    public static String b;

    // :: error: (initialization.fields.uninitialized)
    static {
        a = "";
    }

    public StaticInitializer() {}
}

// :: error: (initialization.fields.uninitialized)
class StaticInitializer2 {
    public static String a;
    public static String b;
}

class StaticInitializer3 {
    public static String a = "";
}

class StaticInitializer4 {
    public static String a = "";
    public static String b;

    static {
        b = "";
    }
}
