import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

class MultiConstructorInit {

    String a;

    public MultiConstructorInit(boolean t) {
        a = "";
    }

    public MultiConstructorInit() {
        this(true);
    }

    // :: error: (initialization.fields.uninitialized)
    public MultiConstructorInit(int t) {
        new MultiConstructorInit();
    }

    // :: error: (initialization.fields.uninitialized)
    public MultiConstructorInit(float t) {}

    public static void main(String[] args) {
        new MultiConstructorInit();
    }
}
