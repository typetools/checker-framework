// @skip-test until the bug is fixed

import java.util.List;
import org.checkerframework.framework.testchecker.h1h2checker.quals.*;

// :: error: [type.annotations.on.location]
public class EnforceTargetLocation<T extends @H2S1 Object> {
  @H2S1 Object right;

  // :: error: [type.annotations.on.location]
  @H2OnlyOnLB Object wrong;

  @H2S1 Object correctUse(@H2S1 Object p1) {
    // :: warning: (cast.unsafe.constructor.invocation)
    @H2S1 Object o = new @H2S1 Object();
    List<? super @H2OnlyOnLB Number> l;
    return o;
  }

  @H2OnlyOnLB
  // :: error: [type.annotations.on.location]
  Object incorrect() {
    // :: warning: (cast.unsafe.constructor.invocation)
    // :: error: [type.annotations.on.location]
    @H2OnlyOnLB Object o = new @H2OnlyOnLB Object();
    return o;
  }

  // :: error: [type.annotations.on.location]
  void incorrectUse2(@H2OnlyOnLB Object p1) {}
}
