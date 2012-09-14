import checkers.nullness.quals.*;

public class Throwing {
    
    String a;
    
    //:: error: (commitment.fields.uninitialized)
    public Throwing(boolean throwError) {
        if (throwError) {
            throw new RuntimeException("not a real error");
        }
    }
    
    //:: error: (commitment.fields.uninitialized)
    public Throwing(int input) {
        try {
            throw new RuntimeException("not a real error");
        } catch (RuntimeException e) {
            // do nothing
        }
    }
    
}