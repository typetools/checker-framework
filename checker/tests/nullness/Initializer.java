import java.util.ArrayList;

import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.framework.qual.*;
import org.checkerframework.checker.initialization.qual.*;

class Initializer {

    public String a;
    public String b = "abc";

    //:: error: (assignment.type.incompatible)
    public String c = null;

    public String d = ("");

    public Initializer() {
        //:: error: (assignment.type.incompatible)
        a = null;
        a = "";
        c = "";
    }

    //:: error: (initialization.fields.uninitialized)
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

    //:: error: (initialization.fields.uninitialized)
    public Initializer(double foo, boolean t) {
        if (!setFieldMaybe()) {
            // on this path, 'a' is not initialized
        }
        c = "";
    }

    @EnsuresQualifier(expression="a", qualifier=NonNull.class)
    public void setField(@UnknownInitialization @Raw Initializer this) {
        a = "";
    }

    @EnsuresQualifierIf(result=true, expression="a", qualifier=NonNull.class)
    public boolean setFieldMaybe(@UnknownInitialization @Raw Initializer this) {
        a = "";
        return true;
    }

    String f = "";
    void t1(@UnknownInitialization @Raw Initializer this) {
        // this is potentially uninitialized, but the static type of f, as well as
        // the initializer guarantee that it is initialized.
        this.f.toString();
    }

}
