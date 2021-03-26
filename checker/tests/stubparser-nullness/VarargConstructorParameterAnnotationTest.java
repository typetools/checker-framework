import org.checkerframework.checker.nullness.qual.*;

/*
 * Tests parsing annotations on parameter represented by an array or vararg to the constructor.
 */
public class VarargConstructorParameterAnnotationTest {

  public void strArraysNonNull(@NonNull String[] parameter) {
    new ProcessBuilder(parameter);
  }

  public void strArraysNullable(@Nullable String[] parameter) {
    // :: error: (argument.type.incompatible)
    new ProcessBuilder(parameter);
  }

  public void strVarargNonNull(@NonNull String... parameter) {
    new ProcessBuilder(parameter);
  }

  public void strVarargNullable(@Nullable String... parameter) {
    // :: error: (argument.type.incompatible)
    new ProcessBuilder(parameter);
  }
}
