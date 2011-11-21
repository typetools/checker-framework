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

        //:: error: (assignment.type.incompatible)
        lRo = lQRo;
        lRo = lRo;
        //:: error: (assignment.type.incompatible)
        lRo = lMut;
        //:: error: (assignment.type.incompatible)
        lRo = lMut2;

        //:: error: (assignment.type.incompatible)
        lMut = lQRo;
        //:: error: (assignment.type.incompatible)
        lMut = lRo;
        lMut = lMut;
        lMut = lMut2;

        //:: error: (assignment.type.incompatible)
        lMut2 = lQRo;
        //:: error: (assignment.type.incompatible)
        lMut2 = lRo;
        lMut2 = lMut;
        lMut2 = lMut2;

    }

}
