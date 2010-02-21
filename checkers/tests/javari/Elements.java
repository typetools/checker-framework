import java.util.*;
import checkers.javari.quals.*;

class Elements {
    List<@QReadOnly Object> lQRo;
    List<@ReadOnly Object> lRo;
    List<@Mutable Object> lMut;
    List<Object> lMut2;

    void foo() {
        lQRo = lQRo;
        lQRo = lRo;
        lQRo = lMut;
        lQRo = lMut2;

        //:: (type.incompatible)
        lRo = lQRo;
        lRo = lRo;
        //:: (type.incompatible)
        lRo = lMut;
        //:: (type.incompatible)
        lRo = lMut2;

        //:: (type.incompatible)
        lMut = lQRo;
        //:: (type.incompatible)
        lMut = lRo;
        lMut = lMut;
        lMut = lMut2;

        //:: (type.incompatible)
        lMut2 = lQRo;
        //:: (type.incompatible)
        lMut2 = lRo;
        lMut2 = lMut;
        lMut2 = lMut2;

    }

}
