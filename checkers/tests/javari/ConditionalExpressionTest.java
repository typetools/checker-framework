import java.util.List;
import checkers.javari.quals.*;

class ConditionalExpressionTest {

    void test() {

        @ReadOnly Object ro = null;
        Object mut = null;
        @PolyRead Object rm = null;

        boolean c = false;

        ro = (c ? ro : ro);
        ro = (c ? ro : mut);
        ro = (c ? ro : rm);
        ro = (c ? mut : ro);
        ro = (c ? mut : mut);
        ro = (c ? mut : rm);
        ro = (c ? rm : ro);
        ro = (c ? rm : mut);
        ro = (c ? rm : rm);

        //:: (type.incompatible)
        mut = (c ? ro : ro);   // error
        //:: (type.incompatible)
        mut = (c ? ro : mut);  // error
        //:: (type.incompatible)
        mut = (c ? ro : rm);   // error
        //:: (type.incompatible)
        mut = (c ? mut : ro);  // error
        mut = (c ? mut : mut);
        //:: (type.incompatible)
        mut = (c ? mut : rm);  // error
        //:: (type.incompatible)
        mut = (c ? rm : ro);   // error
        //:: (type.incompatible)
        mut = (c ? rm : mut);  // error
        //:: (type.incompatible)
        mut = (c ? rm : rm);   // error

        //:: (type.incompatible)
        rm = (c ? ro : ro);    // error
        //:: (type.incompatible)
        rm = (c ? ro : mut);   // error
        //:: (type.incompatible)
        rm = (c ? ro : rm);    // error
        //:: (type.incompatible)
        rm = (c ? mut : ro);   // error
        rm = (c ? mut : mut);
        rm = (c ? mut : rm);
        //:: (type.incompatible)
        rm = (c ? rm : ro);    // error
        rm = (c ? rm : mut);
        rm = (c ? rm : rm);
    }
}
