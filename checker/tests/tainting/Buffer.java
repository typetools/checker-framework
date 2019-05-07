package hardcoded;

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.tainting.qual.PolyTainted;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.framework.qual.HasQualifierParameter;

@HasQualifierParameter(Tainted.class)
public class Buffer {
    final List<@PolyTainted String> list = new ArrayList<>();
    @PolyTainted String someString = "";

    public @PolyTainted Buffer append(@PolyTainted Buffer this, @PolyTainted String s) {
        list.add(s);
        someString = s;
        return this;
    }

    public @PolyTainted String prettyPrint(@PolyTainted Buffer this) {
        String prettyString = "";
        for (String s : list) {
            prettyString += s + " ~~ ";
        }
        return prettyString;
    }

    public @PolyTainted String unTaintedOnly(@Untainted Buffer this, @PolyTainted String s) {
        // :: error: (argument.type.incompatible)
        list.add(s);
        // :: error: (assignment.type.incompatible)
        someString = s;
        return s;
    }
}

class Use {
    void passingUses(@Untainted String untainted, @Untainted Buffer buffer) {
        buffer.list.add(untainted);
        buffer.someString = untainted;
        buffer.append(untainted);
    }

    void failingUses(@Tainted String tainted, @Untainted Buffer buffer) {
        // :: error: (argument.type.incompatible)
        buffer.list.add(tainted);
        // :: error: (assignment.type.incompatible)
        buffer.someString = tainted;
        // TODO: error when @HasQualifierParameter is implemented
        buffer.append(tainted);
    }
}
