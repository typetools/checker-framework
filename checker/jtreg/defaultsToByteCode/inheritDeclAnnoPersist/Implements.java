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
        StringBuilder sb = new StringBuilder();
        sb.append("public Test() {f = new Object();}\n");
        sb.append("@Override public void setf() {f = new Object();}\n");
        sb.append("@Override public void setg() {}\n");
        return TestWrapper.wrap(sb.toString());
    }
}

class TestWrapper {
    public static String wrap(String method) {
        StringBuilder sb = new StringBuilder();
        sb.append("class Test extends AbstractClass {\n");
        sb.append(method);
        sb.append("}");
        return sb.toString();
    }
}
