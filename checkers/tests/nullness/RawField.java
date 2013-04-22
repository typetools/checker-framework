
import checkers.nullness.quals.*;
import checkers.quals.*;
import checkers.initialization.quals.*;

class RawField {
    
    public @Raw @UnkownInitialization RawField a;
    
    public RawField() {
        //:: error: (assignment.type.incompatible)
        a = null;
        this.a = this;
        a = this;
    }
    
    //:: error: (commitment.fields.uninitialized)
    public RawField(boolean foo) {
    }
    
    void t1() {
        //:: error: (method.invocation.invalid)
        a.t1();
    }
    
    void t2(@Raw @UnkownInitialization RawField a) {
        this.a = a;
    }
}
