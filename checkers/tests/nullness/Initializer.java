import java.util.ArrayList;

import checkers.nullness.quals.*;
import checkers.quals.*;
import checkers.initialization.quals.*;

class Initializer {
    
    public String a;
    public String b = "abc";
    
    //:: error: (assignment.type.incompatible)
    public String c = null;
    
    public Initializer() {
        //:: error: (assignment.type.incompatible)
        a = null;
        a = "";
        c = "";
    }
    
    //:: error: (commitment.fields.uninitialized)
    public Initializer(boolean foo) {
    }
    
    public Initializer(int foo) {
        a = "";
        c = "";
    }
    
    public Initializer(float foo) {
        setField();
        c = "";
    }
    
    public Initializer(double foo) {
        if (!setFieldMaybe()) {
            a = "";
        }
        c = "";
    }
    
    //:: error: (commitment.fields.uninitialized)
    public Initializer(double foo, boolean t) {
        if (!setFieldMaybe()) {
            // on this path, 'a' is not initialized
        }
        c = "";
    }
    
    @EnsuresQualifier(expression="a", qualifier=NonNull.class)
    public void setField(@UnkownInitialization @Raw Initializer this) {
        a = "";
    }
    
    @EnsuresQualifierIf(result=true, expression="a", qualifier=NonNull.class)
    public boolean setFieldMaybe(@UnkownInitialization @Raw Initializer this) {
        a = "";
        return true;
    }
    
}
