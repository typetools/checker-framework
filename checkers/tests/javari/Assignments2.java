import checkers.javari.quals.*;
import java.awt.Point;

class Assignments2 {

    Point a = new Point(0, 1);
    @ReadOnly Point b = new Point(2, 3),
        c = new Point(4, 5);
    int i = 0;

    Assignments2 aMutable;
    @ReadOnly Assignments2 aReadOnly;

    Object mObject;
    @ReadOnly Object roObject;

    class JavariCell {
        JavariCell cell;
        int x;

        public void mutateInternal(JavariCell cell) {
            this.cell = cell;
        }

        public void requiresMutableParameter(@ReadOnly JavariCell this, JavariCell cell) {}

        @PolyRead JavariCell getCell(@PolyRead JavariCell this) {
            return cell;
        }
    }

    JavariCell mc;
    @ReadOnly JavariCell roc;

    public @Mutable Object isMutable(@ReadOnly Assignments2 this) {
        return new Object();
    }

    public Object isStillMutable(@ReadOnly Assignments2 this) {
        return new Object();
    }

    public @ReadOnly Object isReadOnly(@ReadOnly Assignments2 this) {
        return "isReadOnly";
    }

    public @PolyRead Object isPolyRead(@PolyRead Object c) {
        return new Object();
    }

    public void cannotDo() {
        //:: error: (type.invalid)
        @ReadOnly int j = 0;   // primitive cannot be annotated as readonly
        //:: error: (assignment.type.incompatible)
        a = b;                 // cannot assign readonly to mutable
        //:: error: (ro.field)
        b.y = i;               // readonly field behave as final
        //:: error: (ro.field)
        b.y = 3;               // readonly field behave as final
        //:: error: (ro.field)
        b.x = a.y;             // readonly field behave as final

        //:: error: (assignment.type.incompatible)
        mObject = isPolyRead(roObject);  // polyread resolved as readonly

        //:: error: (ro.field)
        roc.cell = roc;            // readonly field behave as final
        //:: error: (method.invocation.invalid)
        roc.mutateInternal(mc);    // readonly instance is readonly
        //:: error: (argument.type.incompatible)
        roc.requiresMutableParameter(roc); // requires mutable parameter
        //:: error: (assignment.type.incompatible)
        mc.cell = roc;             // cannot assign readonly to mutable
        //:: error: (assignment.type.incompatible)
        mc.cell = roc.cell;        // cannot assign readonly to mutable
        //:: error: (assignment.type.incompatible)
        mc.cell = roc.getCell();   // cannot assign readonly to mutable

        // cannot assign readonly to mutable
        //:: error: (assignment.type.incompatible)
        mObject = isReadOnly();

        //:: error: (assignment.type.incompatible)
        mObject = aMutable.isReadOnly();
        mObject = aReadOnly.isStillMutable();
        //:: error: (assignment.type.incompatible)
        mObject = aReadOnly.isReadOnly();

        //:: error: (ro.field)
        roc.x = 2;                 // readonly primitive field is final
    }

}
