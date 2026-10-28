import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S1;
import org.checkerframework.framework.testchecker.h1h2checker.quals.H1S2;

public class ReceiverUpperBoundTest {}

class RubtSuper {

  void m(@H1S1 RubtSuper this) {}
}

@SuppressWarnings({"inconsistent.constructor.type", "super.invocation"})
@H1S2 class RubtSub extends RubtSuper {

  @Override
  // :: error: [override.receiver]
  void m(@H1S2 RubtSub this) {}
}

@SuppressWarnings({"inconsistent.constructor.type", "super.invocation"})
@H1S2 class RubtSub2 extends RubtSuper {

  @Override
  // :: error: [override.receiver]
  void m(RubtSub2 this) {}
}
