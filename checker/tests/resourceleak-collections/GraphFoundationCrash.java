import java.util.ArrayList;
import org.checkerframework.checker.collectionownership.qual.*;
import org.checkerframework.checker.mustcall.qual.*;

class CrashRepro {

  // :: error: unfulfilled.field.obligations
  private ArrayList<Resource> pool;

  CrashRepro(int maxSize) {
    this.pool = new ArrayList<>(maxSize); // should trigger the crash path
  }
}
