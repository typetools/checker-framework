import org.checkerframework.common.util.report.qual.*;

public class Inherit {
    @ReportInherit
    interface A {}

    class B {}

    // :: error: (inherit)
    class C extends B implements A {}

    // :: error: (inherit)
    class D extends C {}
}
