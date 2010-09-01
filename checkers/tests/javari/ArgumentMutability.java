import checkers.javari.quals.*;

class ArgumentMutability {

    void roMethod(Object o) @ReadOnly {
        Object x = (Object) o;
        o.hashCode();
    }

}
