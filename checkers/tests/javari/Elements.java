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

        lRo = lQRo;
        lRo = lRo;
        lRo = lMut;
        lRo = lMut2;

        lMut = lQRo;
        lMut = lRo;
        lMut = lMut;
        lMut = lMut2;

        lMut2 = lQRo;
        lMut2 = lRo;
        lMut2 = lMut;
        lMut2 = lMut2;

    }

}
