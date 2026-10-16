// Simple test case to ensure that the CMC can infer facts about parameters with
// type-variable types.

import org.checkerframework.checker.mustcall.qual.*;

public class TypevarSimple {
  public static <T extends java.io.Closeable> void sneakyDropCorrect(
      @Owning @MustCall("close") T value1) throws Exception {
    value1.close();
  }
}
