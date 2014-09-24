import java.util.ArrayList;

import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.framework.qual.*;
import org.checkerframework.checker.initialization.qual.*;

class Initializer {

    public static String a;
    public static String b;

    //:: error: (initialization.fields.uninitialized)
    static {
        a = "";
    }

    public Initializer() {
    }
}

//:: error: (initialization.fields.uninitialized)
class Initializer2 {
    public static String a;
    public static String b;
}

class Initializer3 {
    public static String a = "";
}

class Initializer4 {
    public static String a = "";
    public static String b;
    static {
        b = "";
    }
}
