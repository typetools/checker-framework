import org.checkerframework.checker.nullness.qual.*;

public class NonNullInitialization {
  private String test = "test";

  public static void main(String[] args) {
    NonNullInitialization n = new NonNullInitialization();
    n.test.equals("ASD");
  }
}
