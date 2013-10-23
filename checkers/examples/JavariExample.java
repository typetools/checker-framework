package examples.javari;

import checkers.javari.quals.*;
import java.awt.Point;

class JavariExample {

    Point a = new Point(0, 1);
    @ReadOnly Point b = new Point(2, 3),
        c = new Point(4, 5);
    int i = 0;

    JavariCell mc = new JavariCell();
    @ReadOnly JavariCell roc = new JavariCell();

    String mString;
    @ReadOnly String roString;

    public String isMutable() {
        return "isMutable";
    }

    @ReadOnly public String isReadOnly() {
        return "isReadOnly";
    }

    @PolyRead public String isPolyRead(String doesntMatter,
                                     @PolyRead JavariCell matters) {
        matters.cell = null; // cannot modify PolyRead argument
        return "isPolyRead";
    }

    public void canDo() {
        i = b.x;
        c = b;
        b = a;
        b = (/*@ReadOnly*/ Point) a;
        a = (Point) b; // should emit a warning here
        a.x = 0;
        i = a.x;
        a.x = b.y;   // can assign inherited readonly PRIMITIVE field to mutable
        mc.cell = mc;
        roc = roc.cell;
        roc = null;

        roString = isPolyRead(mString, roc); // polyread resolved as readonly
        mString = isPolyRead(mString, mc);  // polyread resolved as mutable;

        mc.mutateInternal("foo");

        mString = isMutable();

        roString = isReadOnly();
        roString = isMutable();

        mString = mc.isMutable();     // method with mutable receiver w/ mutable reference
        mString = mc.isThisMutable(); // method with thismutable receiver w/ mutable reference
        mString = roc.isMutable();    // method with mutable receiver w/ readonly reference

        roString = mc.isMutable();
        roString = mc.isThisMutable();
        roString = mc.isReadOnly();

        roString = roc.isMutable();
        roString = roc.isThisMutable();
        roString = roc.isReadOnly();
    }

    public void cannotDo() {
        @ReadOnly int j = 0;   // primitive cannot be annotated as readonly
        a = b;                 // cannot assign readonly to mutable
        b.y = i;               // readonly field behave as final
        b.y = 3;               // readonly field behave as final
        b.x = a.y;             // readonly field behave as final
        roc.cell = mc;         // readonly field behave as final
        roc.mutateInternal("foo"); // readonly instance is readonly
        mc.cell = roc;         // cannot assign readonly to mutable
        mc.cell = roc.cell;    // cannot assign inherited readonly non-primitive field to mutable
        mString = isReadOnly(); // cannot put readonly return in mutable variable
        mString = roc.isReadOnly();    // method with readonly receiver w/ readonly reference
        mString = roc.isThisMutable(); // method with thismutable receiver w/ readonly reference
        mString = mc.isReadOnly();     // method with readonly receiver w/ mutable reference
        mString = isPolyRead(mString, roc); // polyread resolved as readonly
    }


    class JavariCell {
        JavariCell cell;
        String s;

        @ReadOnly String isReadOnly() {
            return "isReadOnly";
        }

        String isThisMutable() {
            return "isThisMutable";
        }

        @Mutable String isMutable() {
            return "isMutable";
        }

        String argumentReadOnly(@ReadOnly String s) {
            s = s + " ";   // can reassign? yes, because it is not final
            return s;      // ... but cannot return readonly as mutable.
        }

        void argumentReadOnly(@ReadOnly JavariCell c){
            c.cell = this; // readonly parameter is readonly
            cell = c;      // cannot pass readonly reference to mutable
        }

        // this is ok
        void mutateInternal(String other) /*@Mutable*/ {
            s = other;
            this.s = other;
        }

        // this is not ok
        void illegalMutateInternal(String other) /*@ReadOnly*/ {
            s = other;       // method with readonly receiver, cannot do this
            this.s = other;  // method with readonly readonly, cannot do this either
        }


        @ReadOnly int readonlyIntReturnType() {  // illegal return type
            return 0;
        }
    }

    public class illegalExtensions extends JavariExample {

        @ReadOnly public String isMutable() { // cannot override
            return "isNotReally";
        }

    }

}
