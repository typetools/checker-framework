/*
 * @test
 * @summary Test that inherited declaration annotations are stored in bytecode.
 *
 * @compile ../PersistUtil.java Driver.java ReferenceInfoUtil.java Extends.java Super.java
 * @run main Driver Extends
 */

import static com.sun.tools.classfile.TypeAnnotation.TargetType.*;

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
