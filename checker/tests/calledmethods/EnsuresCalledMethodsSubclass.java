import java.io.*;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;

public class EnsuresCalledMethodsSubclass {

  public static class Parent {
    @EnsuresCalledMethods(value = "#1", methods = "close")
    public void method(Closeable x) throws IOException {
      x.close();
    }
  }

  public static class Subclass extends Parent {
    @Override
    // ::error: (contracts.postcondition)
    public void method(Closeable x) throws IOException {}
  }
}
