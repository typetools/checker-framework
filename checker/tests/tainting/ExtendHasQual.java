import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.framework.qual.HasQualifierParameter;

public class ExtendHasQual {
    static class Super {
        @SuppressWarnings("super.invocation.invalid")
        @Untainted Super() {}
    }

    @HasQualifierParameter(Tainted.class)
    static class Buffer extends Super {}

    static class MyBuffer1 extends Buffer {}

    @HasQualifierParameter(Tainted.class)
    static class MyBuffer2 extends Buffer {}

    @HasQualifierParameter(Nullable.class)
    // :: error: (missing.has.qual.param)
    static class MyBuffer3 extends Buffer {}

    @HasQualifierParameter({Tainted.class, Nullable.class})
    static class MyBuffer4 extends Buffer {}

    @HasQualifierParameter(Tainted.class)
    interface BufferInterface {}

    static class ImplementsBufferInterface1 implements BufferInterface {}

    @HasQualifierParameter(Tainted.class)
    static class ImplementsBufferInterface2 implements BufferInterface {}

    static class Both1 extends Buffer implements BufferInterface {}

    @HasQualifierParameter(Tainted.class)
    static class Both2 extends Buffer implements BufferInterface {}

    static class Both3 extends Super implements BufferInterface {}
}
