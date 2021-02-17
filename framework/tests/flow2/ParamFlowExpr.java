import org.checkerframework.framework.qual.RequiresQualifier;
import org.checkerframework.framework.test.*;
import org.checkerframework.framework.testchecker.util.*;

public class ParamFlowExpr {

    @RequiresQualifier(expression = "#1", qualifier = Odd.class)
    void t1(String p1) {
        String l1 = p1;
    }

    @RequiresQualifier(expression = "#1", qualifier = Odd.class)
    // :: error: (flowexpr.parameter.not.final)
    void t2(String p1) {
        p1 = "";
    }

    @RequiresQualifier(expression = "#1", qualifier = Odd.class)
    public static boolean eltsNonNull(Object[] seq1) {
        if (seq1 == null) {
            return false;
        }
        for (int i = 0; i < seq1.length; i++) {
            if (seq1[i] == null) {
                return false;
            }
        }
        return true;
    }
}
