// Test case for issue #2186
// https://github.com/typetools/checker-framework/issues/2186

import java.util.ArrayList;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1Bot;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S1;

@SuppressWarnings("anno.on.irrelevant")
@H1S1 class Issue2186 {
  // :: error: (super.invocation) :: warning: (inconsistent.constructor.type)
  Issue2186() {}

  // :: error: (super.invocation) :: warning: (inconsistent.constructor.type)
  @H1Bot Issue2186(int x) {}

  void test() {
    @H1S1 Issue2186 obj = new Issue2186();
    @H1Bot Issue2186 obj1 = new Issue2186(9);
  }

  void testDiamond() {
    @H1Bot ArrayList<@H1Bot String> list =
        // :: warning: (cast.unsafe.constructor.invocation)
        new @H1Bot ArrayList<@H1Bot String>();
  }
}
