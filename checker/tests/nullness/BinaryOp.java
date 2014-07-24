import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.initialization.qual.*;

class BinaryOp {
  void test(/*@UnknownInitialization*/ /*@Raw*/ Object obj) {
        throw new Error("" + obj);
  }
}
