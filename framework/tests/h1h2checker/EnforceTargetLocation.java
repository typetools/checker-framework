import java.util.List;
import org.checkerframework.framework.testchecker.h1h2checker.quals.*;

// :: error: [type.annotations.on.location]
public class EnforceTargetLocation<T extends @H2OnlyOnLB Object> {
  @H2OnlyOnConstructorResult
  // :: warning: [inconsistent.constructor.type]
  // :: error: [super.invocation]
  EnforceTargetLocation() {}

  // :: error: [type.annotations.on.location]
  @H2OnlyOnReceiver
  // :: warning: [inconsistent.constructor.type]
  // :: error: [super.invocation]
  EnforceTargetLocation(int ignored) {}

  @H2S1 Object right;

  // :: error: [type.annotations.on.location]
  @H2OnlyOnLB Object wrong;

  @H2S1 Object correctUse(@H2S1 Object p1) {
    // :: warning: (cast.unsafe.constructor.invocation)
    @H2S1 Object o = new @H2S1 Object();
    List<? super @H2OnlyOnLB Number> l;
    return o;
  }

  // :: error: [type.annotations.on.location]
  @H2OnlyOnLB
  Object incorrect() {
    // :: warning: (cast.unsafe.constructor.invocation)
    // :: error: [type.annotations.on.location]
    @H2OnlyOnLB Object o = new @H2OnlyOnLB Object();
    return o;
  }

  // :: error: [type.annotations.on.location]
  void incorrectUse2(@H2OnlyOnLB Object p1) {}

  void receiver(@H2OnlyOnReceiver EnforceTargetLocation<T> this) {}

  // :: error: [type.annotations.on.location]
  void ordinaryParameter(@H2OnlyOnReceiver Object p1) {}
}
