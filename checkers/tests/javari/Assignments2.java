import checkers.javari.quals.*;
import java.awt.Point;

class Assignments2 {

    Point a = new Point(0, 1);
    @ReadOnly Point b = new Point(2, 3),
        c = new Point(4, 5);
    int i = 0;

    Assignments2 aMutable;
    @ReadOnly Assignments2 aReadOnly;

    String mString;
    @ReadOnly String roString;

    class JavariCell {
        JavariCell cell;
        int x;

        public void mutateInternal(JavariCell cell) {
            this.cell = cell;
        }

        public void requiresMutableParameter(JavariCell cell) @ReadOnly {}

        @PolyRead JavariCell getCell() @PolyRead {
            return cell;
        }
    }

    JavariCell mc;
    @ReadOnly JavariCell roc;

    @Mutable public String isMutable() @ReadOnly {
        return "isMutable";
    }

    public String isStillMutable() @ReadOnly {
        return "isStillMutable";
    }

    @ReadOnly public String isReadOnly() @ReadOnly {
        return "isReadOnly";
    }

    @PolyRead public String isPolyRead(@PolyRead Object c) {
        return "isPolyRead";
    }

    public void cannotDo() {
        //:: (primitive.ro)
        @ReadOnly int j = 0;   // primitive cannot be annotated as readonly
        //:: (assignment.type.incompatible)
        a = b;                 // cannot assign readonly to mutable
        //:: (ro.field)
        b.y = i;               // readonly field behave as final
        //:: (ro.field)
        b.y = 3;               // readonly field behave as final
        //:: (ro.field)
        b.x = a.y;             // readonly field behave as final

        //:: (assignment.type.incompatible)
        mString = isPolyRead(roString);  // polyread resolved as readonly

        //:: (ro.field)
        roc.cell = roc;            // readonly field behave as final
        //:: (method.invocation.invalid)
        roc.mutateInternal(mc);    // readonly instance is readonly
        //:: (argument.type.incompatible)
        roc.requiresMutableParameter(roc); // requires mutable parameter
        //:: (assignment.type.incompatible)
        mc.cell = roc;             // cannot assign readonly to mutable
        //:: (assignment.type.incompatible)
        mc.cell = roc.cell;        // cannot assign readonly to mutable
        //:: (assignment.type.incompatible)
        mc.cell = roc.getCell();   // cannot assign readonly to mutable

        // cannot assign readonly to mutable
        //:: (assignment.type.incompatible)
        mString = isReadOnly();

        //:: (assignment.type.incompatible)
        mString = aMutable.isReadOnly();
        mString = aReadOnly.isStillMutable();
        //:: (assignment.type.incompatible)
        mString = aReadOnly.isReadOnly();

        //:: (ro.field)
        roc.x = 2;                 // readonly primitive field is final
    }

}
