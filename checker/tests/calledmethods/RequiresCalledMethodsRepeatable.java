import java.io.*;
import org.checkerframework.checker.calledmethods.qual.*;

public class RequiresCalledMethodsRepeatable {

  @RequiresCalledMethods(value = "#1", methods = "close")
  @RequiresCalledMethods(value = "#2", methods = "close")
  public void requires2(Closeable r1, Closeable r2) {
    @CalledMethods("close") Closeable r3 = r1;
    @CalledMethods("close") Closeable r4 = r2;
  }

  public void requires2Wrong(Closeable r1, Closeable r2) {
    // ::error: (contracts.precondition)
    requires2(r1, r2);
  }

  @RequiresCalledMethods(value = "#1", methods = "close")
  @RequiresCalledMethods(value = "#2", methods = "close")
  public void requires2Correct(Closeable r1, Closeable r2) {
    requires2(r1, r2);
  }

  public static class Subclass extends RequiresCalledMethodsRepeatable {
    @Override
    public void requires2Correct(Closeable r1, Closeable r2) {}

    public void caller(Closeable r1, Closeable r2) {
      requires2Correct(r1, r2); // OK: we override requires2Correct() with a weaker precondition
    }
  }
}
