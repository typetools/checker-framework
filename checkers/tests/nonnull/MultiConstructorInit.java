import checkers.nullness.quals.*;
import checkers.initialization.quals.*;
import static checkers.nullness.util.NonNullUtils.*;

class MultiConstructorInit {
    
    String a;
    
    public MultiConstructorInit(boolean t) {
        a = "";
    }
    
    public MultiConstructorInit() {
        this(true);
    }
    
    //:: error: (commitment.fields.uninitialized)
    public MultiConstructorInit(int t) {
        new MultiConstructorInit();
    }
    
    //:: error: (commitment.fields.uninitialized)
    public MultiConstructorInit(float t) {
    }
    
    public static void main(String[] args) {
        new MultiConstructorInit();
    }
}
