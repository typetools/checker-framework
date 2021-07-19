import org.checkerframework.common.returnsreceiver.qual.*;

public class MethodRef {

    @This MethodRef set(Object o) {
        return this;
    }

    interface Setter {
        @This Object consume(Object p);
    }

    // :: error: methodref.receiver.bound.invalid
    Setter co = this::set;

    void doNothing(@This MethodRef this) {}

    interface Fun {
        void run(@This Fun this);
    }

    // The error here is a false positive, due to
    // https://github.com/typetools/checker-framework/issues/2931
    // :: error: methodref.receiver.bound.invalid
    Fun f = this::doNothing;
}
