import checkers.nullness.quals.*;
import checkers.initialization.quals.*;

class MultiConstructorInit {

    String a;

    public MultiConstructorInit(boolean t) {
        a = "";
    }

    public MultiConstructorInit() {
        this(true);
    }

    //:: error: (initialization.fields.uninitialized)
    public MultiConstructorInit(int t) {
        new MultiConstructorInit();
    }

    //:: error: (initialization.fields.uninitialized)
    public MultiConstructorInit(float t) {
    }

    public static void main(String[] args) {
        new MultiConstructorInit();
    }
}
