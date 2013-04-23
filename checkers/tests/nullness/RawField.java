
import checkers.nullness.quals.*;
import checkers.quals.*;
import checkers.initialization.quals.*;

class RawField {
    
    public @Raw @UnknownInitialization RawField a;
    
    public RawField() {
        //:: error: (assignment.type.incompatible)
        a = null;
        this.a = this;
        a = this;
    }
    
    //:: error: (initialization.fields.uninitialized)
    public RawField(boolean foo) {
    }
    
    void t1() {
        //:: error: (method.invocation.invalid)
        a.t1();
    }
    
    void t2(@Raw @UnknownInitialization RawField a) {
        this.a = a;
    }
}
