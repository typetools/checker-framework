package flowexpression;

import testlib.flowexpression.qual.FlowExp;

public class DoubleParsing {
    void method(@FlowExp("1.0") Object doubleValue) {
        @FlowExp("1.0") Object value = doubleValue;
    }
}
