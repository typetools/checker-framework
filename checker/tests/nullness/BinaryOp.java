import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

class BinaryOp {
    void test(@UnknownInitialization Object obj) {
        throw new Error("" + obj);
    }
}
