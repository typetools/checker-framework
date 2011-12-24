import checkers.javari.quals.*;
import java.awt.Point;

class Assignments {

    Point a = new Point(0, 1);
    @ReadOnly Point b = new Point(2, 3),
        c = new Point(4, 5);
    int i = 0;

    Assignments aMutable;
    @ReadOnly Assignments aReadOnly;

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

    public @Mutable Object isMutable(@ReadOnly Assignments this) {
        return new Object();
    }

    public Object isThisMutable(@ReadOnly Assignments this) {
        return new Object();
    }

    public @ReadOnly Object isReadOnly(@ReadOnly Assignments this) {
        return "isReadOnly";
    }

    public @PolyRead Object isPolyRead(@PolyRead Object c) {
        return new Object();
    }

    public void canDo() {
        i = b.x;
        c = b;
        b = a;
        b = (@ReadOnly Point) a;
        //:: warning: (cast.unsafe)
        a = (Point) b; // should emit a warning here
        a.x = 0;
        i = a.x;
        a.x = b.y;   // can assign PRIMITIVE field
        mc.cell = mc;
        roc = roc.cell;
        roc = null;
        mc.mutateInternal(mc);
        mc.requiresMutableParameter(mc);
        roc.requiresMutableParameter(mc);
        mc = mc.getCell();
        roc = roc.getCell();

        roObject = isPolyRead(roObject); // polyread resolved as readonly
        mObject = isPolyRead(mObject);   // polyread resolved as mutable;
        roObject = isPolyRead(mObject);  // polyread resolved as mutable;

        mObject = isMutable();
        mObject = isThisMutable();

        roObject = isMutable();
        roObject = isThisMutable();
        roObject = isReadOnly();


        // mutable can be assigned to mutable
        mObject = aMutable.isMutable();
        mObject = aMutable.isThisMutable();
        mObject = aReadOnly.isMutable();

        // anything can be passed to a readonly
        roObject = aMutable.isMutable();
        roObject = aMutable.isThisMutable();
        roObject = aMutable.isReadOnly();
        roObject = aReadOnly.isMutable();
        roObject = aReadOnly.isThisMutable();
        roObject = aReadOnly.isReadOnly();
    }

    public void doNothing(@ReadOnly Assignments this) {
        JavariCell localCell = null;      // mutable
        localCell.x = 3;
        localCell.cell = localCell;            // is mutable, no error
    }

    public int getInt(@ReadOnly Assignments this) { return 0; }
    public void assignmentWithCast(@ReadOnly Assignments this) {
        char c = (char) getInt();
    }
}
