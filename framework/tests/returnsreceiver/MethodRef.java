import org.checkerframework.common.returnsreceiver.qual.*;

class MethodRef {

    @This MethodRef set(Object o) {
        return this;
    }

    interface Setter {
        @This Object consume(Object p);
    }

    // :: error: (methodref.receiver.bound.invalid)
    Setter co = this::set;
}
