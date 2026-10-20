// @infer-jaifs-skip-test The AFU's JAIF reading/writing libraries don't support records.
// @infer-stubs-skip-test This test outputs a warning about records.
// @below-java17-jdk-skip-test
package issue6069;

public class Issue6069 {
  public interface MyInterface {
    Record foo();
  }

  public static class MyClass implements MyInterface {

    public MyRecord foo() {
      return new MyRecord(42);
    }

    record MyRecord(int bar) {}

    public static void main(String[] args) {
      MyClass f = new MyClass();
      System.out.println(f.foo());
    }
  }
}
