import org.checkerframework.checker.javari.qual.*;

class ArgumentMutability {

    void roMethod(@ReadOnly ArgumentMutability this, Object o) {
        Object x = (Object) o;
        o.hashCode();
    }

}
