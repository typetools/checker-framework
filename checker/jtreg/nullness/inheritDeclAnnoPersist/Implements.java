import org.checkerframework.javacutil.PluginUtil;

/*
 * @test
 * @summary Test that inherited declaration annotations are stored in bytecode.
 *
 * @compile ../PersistUtil.java Driver.java ReferenceInfoUtil.java Implements.java AbstractClass.java
 * @run main Driver Implements
 */

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
        return PluginUtil.joinLines(
                "class Test extends AbstractClass {", PluginUtil.joinLines(method), "}");
    }
}
