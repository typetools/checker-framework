package use;

import lib.Test;

import org.checkerframework.framework.testchecker.variablenamedefault.quals.*;

public class UseTest {
    void testParamters(@VariableNameDefaultTop int t) {
        Test.method(t, t);
    }
}
