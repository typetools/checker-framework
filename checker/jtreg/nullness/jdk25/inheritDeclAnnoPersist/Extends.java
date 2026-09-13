/*
 * @test
 * @requires jdk.version.major >= 25
 * @summary Test that inherited declaration annotations are stored in bytecode.
 *
 * @compile ../PersistUtil.java Driver.java ReferenceInfoUtil.java Extends.java Super.java
 * @run main Driver Extends
 */

// Keep in sync with ../../jdk24/inheritDeclAnnoPersist/Extends.java, which uses the
// com.sun.tools.classfile API that was removed in Java 25.  This version uses the
// java.lang.classfile API.

public class Extends {

  @ADescriptions({
    @ADescription(annotation = "org/checkerframework/checker/nullness/qual/EnsuresNonNull")
  })
  public String m1() {
    StringBuilder sb = new StringBuilder();
    return TestWrapper.wrap("@Override void setf() { f = new Object(); }");
  }

  @ADescriptions({})
  public String m2() {
    return TestWrapper.wrap("@Override void setg() {}");
  }

  // Issue 342
  // We do not want that behavior with related annotations. @Pure should override @SideEffectFree.
  @ADescriptions({
    @ADescription(annotation = "org/checkerframework/dataflow/qual/Pure"),
    @ADescription(annotation = "org/checkerframework/dataflow/qual/SideEffectFree")
  })
  public String m3() {
    return TestWrapper.wrap("@Pure @Override void seth() {}");
  }
}

class TestWrapper {
  public static String wrap(String... method) {
    return "class Test extends Super {"
        + System.lineSeparator()
        + String.join(System.lineSeparator(), method)
        + System.lineSeparator()
        + "}";
  }
}
