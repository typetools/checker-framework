// Simple test case to ensure that the CMC can infer facts about parameters with
// type-variable types.

import org.checkerframework.checker.calledmethods.qual.CalledMethods;

public class TypevarSimple {
  public static <T extends java.io.Closeable> void sneakyDropCorrect(
      T value1) throws Exception {
    value1.close();
    @CalledMethods("close") T t = value1;
  }
}
