import java.io.*;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;

class EnsuresCalledMethodsRepeatable {

  @EnsuresCalledMethods(
      value = "#1",
      methods = {"toString"})
  @EnsuresCalledMethods(
      value = "#1",
      methods = {"hashCode"})
  void test(Object obj) {
    obj.toString();
    obj.hashCode();
  }

  @EnsuresCalledMethods(value = "#1", methods = "close")
  @EnsuresCalledMethods(value = "#2", methods = "close")
  // ::error: (contracts.postcondition)
  public void close2MissingFirst(Closeable r1, Closeable r2) throws IOException {
    r1.close();
  }

  @EnsuresCalledMethods(value = "#1", methods = "close")
  @EnsuresCalledMethods(value = "#2", methods = "close")
  // ::error: (contracts.postcondition)
  public void close2MissingSecond(Closeable r1, Closeable r2) throws IOException {
    r2.close();
  }

  @EnsuresCalledMethods(value = "#1", methods = "close")
  @EnsuresCalledMethods(value = "#2", methods = "close")
  public void close2Correct(Closeable r1, Closeable r2) throws IOException {
    try {
      r1.close();
    } finally {
      r2.close();
    }
  }

  @EnsuresCalledMethods(value = "#1", methods = "close")
  @EnsuresCalledMethods(value = "#2", methods = "close")
  public void close2CorrectViaCall(Closeable r1, Closeable r2) throws IOException {
    close2Correct(r1, r2);
  }

  public static class Subclass extends EnsuresCalledMethodsRepeatable {
    @Override
    // ::error: (contracts.postcondition)
    public void close2Correct(Closeable r1, Closeable r2) throws IOException {}
  }
}
