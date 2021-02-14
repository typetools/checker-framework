package flowexpression;

import org.checkerframework.framework.testchecker.flowexpression.qual.FlowExp;

public class CharAndDoubleParsing {
    void doubleParsing(@FlowExp("1.0") Object doubleValue) {
        @FlowExp("1.0") Object value = doubleValue;
    }

    void CharParsing(@FlowExp("'c'") Object charValue) {
        @FlowExp("'c'") Object value = charValue;
    }
}
