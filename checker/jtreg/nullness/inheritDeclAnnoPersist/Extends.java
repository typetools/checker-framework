/*
 * @test
 * @summary Test that inherited declaration annotations are stored in bytecode.
 *
 * @compile -source 7 -target 7 ../PersistUtil.java Driver.java ReferenceInfoUtil.java Extends.java Super.java
 * @run main Driver Extends
 */

public class Extends {

    @ADescriptions({
        @ADescription(annotation = "org/checkerframework/checker/nullness/qual/EnsuresNonNull")
    })
    public String m1() {
        StringBuilder sb = new StringBuilder();
        return TestWrapper.wrap("@Override void setf() {f = new Object();}\n");
    }

    @ADescriptions({})
    public String m2() {
        return TestWrapper.wrap("@Override void setg() {}\n");
    }

    // Issue 342
    // We do not want that behavior with related annotations. @Pure should
    // override @SideEffectFree.
    @ADescriptions({
        @ADescription(annotation = "org/checkerframework/dataflow/qual/Pure"),
        @ADescription(annotation = "org/checkerframework/dataflow/qual/SideEffectFree")
    })
    public String m3() {
        return TestWrapper.wrap("@Pure @Override void seth() {}\n");
    }
}

class TestWrapper {
    public static String wrap(String method) {
        StringBuilder sb = new StringBuilder();
        sb.append("class Test extends Super {\n");
        sb.append(method);
        sb.append("}");
        return sb.toString();
    }
}
