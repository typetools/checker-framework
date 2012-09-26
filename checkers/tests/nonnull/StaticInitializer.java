import java.util.ArrayList;

import checkers.nonnull.quals.*;
import checkers.quals.*;
import checkers.initialization.quals.*;

class Initializer {
    
    public static String a;
    public static String b;
    
    //:: error: (commitment.fields.uninitialized)
    static {
        a = "";
    }
    
    public Initializer() {
    }
}

//:: error: (commitment.fields.uninitialized)
class Initializer2 {
    public static String a;
    public static String b;
}
