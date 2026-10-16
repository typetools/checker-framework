package use;

import lib.Test;
import org.checkerframework.framework.testchecker.variablenamedefault.quals.*;

public class UseTest {
  void testParameters(@VariableNameDefaultTop int t) {
    Test.method(t, t);
  }
}
