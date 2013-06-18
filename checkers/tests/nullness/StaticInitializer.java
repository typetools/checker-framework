import java.util.ArrayList;

import checkers.nullness.quals.*;
import checkers.quals.*;
import checkers.initialization.quals.*;

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
