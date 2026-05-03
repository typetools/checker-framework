// Examples from https://openjdk.org/jeps/513.
// @below-java25-jdk-skip-test
// None of the WPI formats supports the new Java 25 languages features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test
@SuppressWarnings("all") // Just check for crashes.
public class Jep513 {
  class Person {

    int age;

    Person(int age) {
      if (age < 0) {
        throw new IllegalArgumentException("...");
      }
      this.age = age;
    }
  }

  class Employee extends Person {

    String officeID;

    Employee(int age, String officeID) {
      if (age < 18 || age > 67) {
        throw new IllegalArgumentException("...");
      }
      this.officeID = officeID;
      super(age);
    }
  }

  static class Outer {

    int i;

    void hello() {
      System.out.println("Hello");
    }

    class Inner {

      int j;

      Inner() {
        var x = i; // OK - implicitly refers to field of enclosing instance
        var y = Outer.this.i; // OK - explicitly refers to field of enclosing instance
        hello(); // OK - implicitly refers to method of enclosing instance
        Outer.this.hello(); // OK - explicitly refers to method of enclosing instance
        super();
      }
    }
  }
}
