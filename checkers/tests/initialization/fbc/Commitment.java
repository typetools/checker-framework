import checkers.nonnull.quals.*;
import checkers.initialization.quals.*;

public class Commitment {

    @NonNull String t;
    
    //:: error: (commitment.invalid.field.annotation)
    @NonNull @Free String a;
    //:: error: (commitment.invalid.field.annotation)
    @Committed String b;
    //:: error: (commitment.invalid.field.annotation)
    @Unclassified @Nullable String c;
    
    //:: error: (commitment.invalid.constructor.return.type)
    public @Free Commitment(int i) {
        a = "";
        t = "";
        b = "";
    }

    //:: error: (commitment.invalid.constructor.return.type)
    public @Committed Commitment(int i, int j) {
        a = "";
        t = "";
        b = "";
    }
    
    //:: error: (commitment.invalid.constructor.return.type)
    public @Committed @NonNull Commitment(boolean i) {
        a = "";
        t = "";
        b = "";
    }
    
    //:: error: (commitment.invalid.constructor.return.type)
    public @Nullable Commitment(char i) {
        a = "";
        t = "";
        b = "";
    }
    
    //:: error: (commitment.fields.uninitialized)
    public Commitment() {
        //:: error: (dereference.of.nullable)
        t.toLowerCase();
        
        t = "";
        
        @Free @NonNull Commitment c = this;

        @Unclassified @NonNull Commitment c1 = this;

        //:: error: (assignment.type.incompatible)
        @Committed @NonNull Commitment c2 = this;
    }

    //:: error: (commitment.fields.uninitialized)
    public Commitment(@Unclassified Commitment arg) {
        t = "";
        
        //:: error: (argument.type.incompatible)
        @Free Commitment t = new Commitment(this, 1);

        //:: error: (assignment.type.incompatible)
        @Committed Commitment t1 = new Commitment(this);
        
        @Free Commitment t2 = new Commitment(this);
    }
    
    //:: error: (commitment.fields.uninitialized)
    public Commitment(Commitment arg, int i) {

    }
    
}
