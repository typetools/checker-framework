import java.io.*;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethodsIf;

public class EnsuresCalledMethodsIfRepeatable {

  @EnsuresCalledMethodsIf(expression = "#1", result = true, methods = "close")
  @EnsuresCalledMethodsIf(expression = "#2", result = true, methods = "close")
  public boolean close2MissingFirst(Closeable r1, Closeable r2) throws IOException {
    r1.close();
    // ::error: (contracts.conditional.postcondition)
    return true;
  }

  @EnsuresCalledMethodsIf(expression = "#1", result = true, methods = "close")
  @EnsuresCalledMethodsIf(expression = "#2", result = true, methods = "close")
  public boolean close2MissingSecond(Closeable r1, Closeable r2) throws IOException {
    r2.close();
    // ::error: (contracts.conditional.postcondition)
    return true;
  }

  @EnsuresCalledMethodsIf(expression = "#1", result = true, methods = "close")
  @EnsuresCalledMethodsIf(expression = "#2", result = true, methods = "close")
  public boolean close2Correct(Closeable r1, Closeable r2) throws IOException {
    try {
      r1.close();
    } finally {
      r2.close();
    }
    return true;
  }

  @EnsuresCalledMethodsIf(expression = "#1", result = true, methods = "close")
  @EnsuresCalledMethodsIf(expression = "#2", result = true, methods = "close")
  public boolean close2CorrectViaCall(Closeable r1, Closeable r2) throws IOException {
    return close2Correct(r1, r2);
  }

  public static class SubclassWrong extends EnsuresCalledMethodsIfRepeatable {
    @Override
    public boolean close2Correct(Closeable r1, Closeable r2) throws IOException {
      // ::error: (contracts.conditional.postcondition)
      return true;
    }
  }

  public static class SubclassRight extends EnsuresCalledMethodsIfRepeatable {
    @Override
    public boolean close2Correct(Closeable r1, Closeable r2) throws IOException {
      return false;
    }
  }
}
