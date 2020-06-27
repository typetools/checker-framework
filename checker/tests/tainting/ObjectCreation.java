import org.checkerframework.checker.tainting.qual.PolyTainted;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.framework.qual.HasQualifierParameter;

public class ObjectCreation {

    @HasQualifierParameter(Tainted.class)
    static class Buffer { // Which constructors and super calls are legal?
        @PolyTainted Buffer() {
            super(); // ok this creates an @Untainted object
        }

        @Untainted Buffer(int p) {
            super(); // ok, super is untainted and creating an untainted buffer
        }

        @Tainted Buffer(String s) {
            super(); // ok, super is not @HasQualifierParameter @Tainted Object >: the type of
            // super.
        }
    }

    @HasQualifierParameter(Tainted.class)
    static class MyBuffer extends Buffer {
        @PolyTainted MyBuffer() {
            super(); // ok, if super is @PolyTainted.
        }

        @Untainted MyBuffer(int p) {
            super(p);
        }

        @Tainted MyBuffer(String s) {
            super(s);
        }

        @PolyTainted MyBuffer(Object o) {
            // :: error: (super.invocation.invalid)
            super("");
        }

        @Untainted MyBuffer(Object o, int p) {
            super();
        }

        @Tainted MyBuffer(Object o, String s) {
            super();
        }
    }
}
