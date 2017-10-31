import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

// This test does not correctly work in the FBC system,
// see https://github.com/typetools/checker-framework/issues/223
class RawField {

    public @Raw @UnknownInitialization RawField a;

    public RawField() {
        // :: error: (assignment.type.incompatible)
        a = null;
        this.a = this;
        a = this;
    }

    // :: error: (initialization.fields.uninitialized)
    public RawField(boolean foo) {}

    void t1() {
        // :: error: (method.invocation.invalid)
        a.t1();
    }

    void t2(@Raw @UnknownInitialization RawField a) {
        this.a = a;
    }
}

class Options {

    @UnknownInitialization @Raw Object arg;

    public Options(@UnknownInitialization @Raw Object arg) {
        this.arg = arg;
    }

    public void parse_or_usage() {
        // use arg only under the assumption that it is @UnknownInitialization
    }
}

class MultiVersionControl {

    @SuppressWarnings("fbc") // see https://github.com/typetools/checker-framework/issues/223
    public void parseArgs(@UnknownInitialization @Raw MultiVersionControl this) {
        Options options = new Options(this);
        options.parse_or_usage();
    }
}

// TODO: This checks that forbidden field assignments do not occur.  (The
// FBC type system permits arbitrary assignments in a constructor, but it
// also makes assumptions that our implementation does not currently check.)
// class HasStaticUnknownInitializationField {
//     static @UnknownInitialization @Raw Object f;
// }
//
// class UseUnknownInitializationField {
//
//     Object f;
//
//     public UseUnknownInitializationField() {
//         // :: (initialization.invalid.field.write.in.constructor)
//         f = HasStaticUnknownInitializationField.f;
//     }
//
// }
