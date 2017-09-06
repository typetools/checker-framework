import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

class BinaryOp {
    void test(@UnknownInitialization @Raw Object obj) {
        throw new Error("" + obj);
    }
}
