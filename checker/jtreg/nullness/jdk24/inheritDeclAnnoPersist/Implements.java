/*
 * @test
 * @requires jdk.version.major <= 24
 * @summary Test that inherited declaration annotations are stored in bytecode.
 *
 * @compile ../PersistUtil.java Driver.java ReferenceInfoUtil.java Implements.java AbstractClass.java
 * @run main Driver Implements
 */

// Keep in sync with ../../jdk25/inheritDeclAnnoPersist/Implements.java, which uses the
// java.lang.classfile API in place of the com.sun.tools.classfile API that was removed in Java 25.

public class Implements {

  @ADescriptions({
    @ADescription(annotation = "org/checkerframework/checker/nullness/qual/EnsuresNonNull")
  })
  public String m1() {
    return TestWrapper.wrap(
        "public Test() { f = new Object(); }",
        "@Override public void setf() { f = new Object(); }",
        "@Override public void setg() {}");
  }
}

class TestWrapper {
  public static String wrap(String... method) {
    return String.join(
        System.lineSeparator(),
        "class Test extends AbstractClass {",
        String.join(System.lineSeparator(), method),
        "}");
  }
}
