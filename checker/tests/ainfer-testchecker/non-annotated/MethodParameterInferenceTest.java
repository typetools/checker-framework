import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

// TODO: Like this one, some tests must verify that it contains the expected
// output after performing the whole-program inference.
public class MethodParameterInferenceTest {
  void foo(int i) {
    i = getAinferSibling1(); // The type of i must be inferred to @AinferSibling1.
  }

  @AinferSibling1 int getAinferSibling1() {
    return (@AinferSibling1 int) 0;
  }
}
