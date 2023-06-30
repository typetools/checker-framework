package issue6069;

// @below-java17-jdk-skip-test
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
