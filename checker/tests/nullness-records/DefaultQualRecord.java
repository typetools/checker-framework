import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

class StandardQualClass {
    // :: error: assignment
    public static String s = null;
    // :: error: initialization.static.field.uninitialized
    public static String u;
}

@DefaultQualifier(Nullable.class)
class DefaultQualClass {
    public static String s = null;
    public static String u;
}

interface StandardQualInterface {
    // :: error: assignment
    public static String s = null;
}

@DefaultQualifier(Nullable.class)
interface DefaultQualInterface {
    public static String s = null;
}

enum StandardQualEnum {
    DUMMY;
    // :: error: assignment
    public static String s = null;
    // :: error: initialization.static.field.uninitialized
    public static String u;
}

@DefaultQualifier(Nullable.class)
enum DefaultQualEnum {
    DUMMY;
    public static String s = null;
    public static String u;
}

record StandardQualRecord(String m) {
    // :: error: assignment
    public static String s = null;
    // :: error: initialization.static.field.uninitialized
    public static String u;

    StandardQualRecord {
        // :: error: assignment
        m = null;
    }
}

@DefaultQualifier(Nullable.class)
record DefaultQualRecord(String m) {
    public static String s = null;
    public static String u;

    DefaultQualRecord {
        m = null;
    }
}
