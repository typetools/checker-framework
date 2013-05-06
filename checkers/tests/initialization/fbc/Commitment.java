import checkers.nullness.quals.*;
import checkers.initialization.quals.*;

public class Commitment {

    @NonNull String t;
    
    //:: error: (commitment.invalid.constructor.return.type)
    @NonNull @UnderInitializion String a;
    //:: error: (commitment.invalid.constructor.return.type)
    @Initialized String b;
    @UnknownInitialization @Nullable String c;
    
    //:: error: (initialization.invalid.constructor.return.type)
    public @UnderInitializion Commitment(int i) {
        a = "";
        t = "";
        b = "";
    }

    //:: error: (initialization.invalid.constructor.return.type)
    public @Initialized Commitment(int i, int j) {
        a = "";
        t = "";
        b = "";
    }
    
    //:: error: (initialization.invalid.constructor.return.type)
    public @Initialized @NonNull Commitment(boolean i) {
        a = "";
        t = "";
        b = "";
    }
    
    //:: error: (initialization.invalid.constructor.return.type)
    public @Nullable Commitment(char i) {
        a = "";
        t = "";
        b = "";
    }
    
    //:: error: (initialization.fields.uninitialized)
    public Commitment() {
        //:: error: (dereference.of.nullable)
        t.toLowerCase();
        
        t = "";
        
        @UnderInitializion @NonNull Commitment c = this;

        @UnknownInitialization @NonNull Commitment c1 = this;

        //:: error: (assignment.type.incompatible)
        @Initialized @NonNull Commitment c2 = this;
    }

    //:: error: (initialization.fields.uninitialized)
    public Commitment(@UnknownInitialization Commitment arg) {
        t = "";
        
        //:: error: (argument.type.incompatible)
        @UnderInitializion Commitment t = new Commitment(this, 1);

        //:: error: (assignment.type.incompatible)
        @Initialized Commitment t1 = new Commitment(this);
        
        @UnderInitializion Commitment t2 = new Commitment(this);
    }
    
    //:: error: (initialization.fields.uninitialized)
    public Commitment(Commitment arg, int i) {

    }
    
}
