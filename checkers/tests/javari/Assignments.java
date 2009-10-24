import checkers.javari.quals.*;
import java.awt.Point;

class Assignments {

    Point a = new Point(0, 1);
    @ReadOnly Point b = new Point(2, 3),
        c = new Point(4, 5);
    int i = 0;

    Assignments aMutable;
    @ReadOnly Assignments aReadOnly;

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

    public String isThisMutable() @ReadOnly {
        return "isThisMutable";
    }

    @ReadOnly public String isReadOnly() @ReadOnly {
        return "isReadOnly";
    }

    @PolyRead public String isPolyRead(@PolyRead Object c) {
        return "isPolyRead";
    }

    public void canDo() {
        i = b.x;
        c = b;
        b = a;
        b = (@ReadOnly Point) a;
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

        roString = isPolyRead(roString); // polyread resolved as readonly
        mString = isPolyRead(mString);   // polyread resolved as mutable;
        roString = isPolyRead(mString);  // polyread resolved as mutable;

        mString = isMutable();
        mString = isThisMutable();

        roString = isMutable();
        roString = isThisMutable();
        roString = isReadOnly();


        // mutable can be assigned to mutable
        mString = aMutable.isMutable();
        mString = aMutable.isThisMutable();
        mString = aReadOnly.isMutable();

        // anything can be passed to a readonly
        roString = aMutable.isMutable();
        roString = aMutable.isThisMutable();
        roString = aMutable.isReadOnly();
        roString = aReadOnly.isMutable();
        roString = aReadOnly.isThisMutable();
        roString = aReadOnly.isReadOnly();
    }

    public void doNothing() @ReadOnly {
        JavariCell localCell = null;      // mutable
        localCell.x = 3;
        localCell.cell = localCell;            // is mutable, no error
    }

    public int getInt() @ReadOnly { return 0; }
    public void assignmentWithCast() @ReadOnly {
        char c = (char) getInt();
    }
}
