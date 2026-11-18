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

  @H2OnlyOnReturn
  Object returnOnly(Object value) {
    // :: warning: [cast.unsafe]
    // :: error: [type.annotations.on.location]
    Object cast = (@H2OnlyOnReturn Object) value;
    // :: warning: [instanceof.unsafe]
    // :: error: [type.annotations.on.location]
    boolean instanceOf = value instanceof @H2OnlyOnReturn String;
    // :: error: [type.annotations.on.location]
    if (value instanceof @H2OnlyOnReturn String pattern) {
      return pattern;
    }
    return cast;
  }

  // A type argument or array component type is not at the location of its enclosing declaration.

  // :: error: [type.annotations.on.location]
  List<@H2OnlyOnReturn Object> typeArgumentOfReturn() {
    return null;
  }

  void typeArgumentInBody() {
    // :: error: [type.annotations.on.location]
    class Local extends java.util.ArrayList<@H2OnlyOnReturn Object> {}
  }

  // :: error: [type.annotations.on.location]
  @H2OnlyOnReturn
  Object[] arrayComponentOfReturn() {
    return new Object[0];
  }

  Object @H2OnlyOnReturn [] arrayReturn() {
    return null;
  }
}
