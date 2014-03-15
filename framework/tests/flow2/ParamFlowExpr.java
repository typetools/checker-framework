import org.checkerframework.framework.test.*;

import java.util.*;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.framework.qual.*;
import tests.util.*;

class ParamFlowExpr {

    @RequiresQualifier(expression="#1", qualifier=Odd.class)
    void t1(String p1) {
        String l1 = p1;
    }

    @RequiresQualifier(expression="#1", qualifier=Odd.class)
    //:: error: (flowexpr.parameter.not.final)
    void t2(String p1) {
        p1 = "";
    }
}
