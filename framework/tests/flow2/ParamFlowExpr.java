import java.util.*;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.framework.qual.*;
import org.checkerframework.framework.test.*;
import tests.util.*;

class ParamFlowExpr {

    @RequiresQualifier(expression = "#1", qualifier = Odd.class)
    void t1(final String p1) {
        String l1 = p1;
    }

    @RequiresQualifier(expression = "#1", qualifier = Odd.class)
    //:: error: (flowexpr.parameter.not.final)
    void t2(String p1) {
        p1 = "";
    }

    @RequiresQualifier(expression = "#1", qualifier = Odd.class)
    public static boolean eltsNonNull(final Object[] seq1) {
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
