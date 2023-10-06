import java.io.*;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethodsOnException;

public class EnsuresCalledMethodsOnExceptionSubclass {

  public static class Parent {
    @EnsuresCalledMethodsOnException(value = "#1", methods = "close")
    public void method(Closeable x) throws IOException {
      x.close();
    }
  }

  public static class SubclassWrong extends Parent {
    @Override
    // ::error: (contracts.postcondition)
    public void method(Closeable x) throws IOException {
      throw new IOException();
    }
  }

  public static class SubclassCorrect extends Parent {
    @Override
    public void method(Closeable x) throws IOException {
      // No exception thrown ==> no contract to satisfy!
    }
  }
}
