import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

public class StaticInitializer {

    public static String a;
    // :: error: (initialization.static.field.uninitialized)
    public static String b;

    static {
        a = "";
    }

    public StaticInitializer() {}
}

class StaticInitializer2 {
    // :: error: (initialization.static.field.uninitialized)
    public static String a;
    // :: error: (initialization.static.field.uninitialized)
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

class StaticInitializer5 {
    public static String a = "";

    static {
        a.toString();
    }

    public static String b = "";
}

class StaticInitializer6 {
    public static String a = "";

    public static String b;

    static {
        // TODO error expected. See #556.
        b.toString();
    }

    static {
        b = "";
    }
}
