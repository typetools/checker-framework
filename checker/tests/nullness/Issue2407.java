import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

public class Issue2407 {

  @RequiresNonNull("#1")
  void setMessage(String message) {}

  @EnsuresNonNull("1")
  // :: error: (contracts.precondition)
  void method() {}

  @EnsuresNonNullIf(expression = "1", result = true)
  // :: error: (contracts.precondition)
  void method2() {}

  void main() {
    Issue2407 object = new Issue2407();
    // :: error: (contracts.precondition)
    object.setMessage(new Object() + "bar");
  }
}
