import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.framework.qual.HasQualifierParameter;
import org.checkerframework.framework.qual.NoQualifierParameter;

@HasQualifierParameter(Tainted.class)
@NoQualifierParameter(Tainted.class)
// :: error: (conflicting.qual.param)
public class TestNoQualifierParameterConflicting {

    @HasQualifierParameter(Tainted.class)
    static class Super {}

    @NoQualifierParameter(Tainted.class)
    // :: error: (conflicting.qual.param)
    static class Sup extends Super {}
}
