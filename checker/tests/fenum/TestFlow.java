import org.checkerframework.checker.fenum.qual.Fenum;

@SuppressWarnings("fenum:assignment")
public class TestFlow {
  public final @Fenum("A") Object ACONST1 = new Object();

  public final @Fenum("B") Object BCONST1 = new Object();
}

class FenumUser {

  @Fenum("A") Object state1 = new TestFlow().ACONST1;

  void foo(TestFlow t) {
    // :: error: (method.invocation)
    state1.hashCode();
    state1 = null;
    state1.hashCode();
    m();
    // :: error: (method.invocation)
    state1.hashCode();

    Object o = new Object();
    o = t.ACONST1;
    methodA(o);
    // :: error: (argument)
    methodB(o);

    o = t.BCONST1;
    // :: error: (argument)
    methodA(o);
    methodB(o);
  }

  void m() {}

  void methodA(@Fenum("A") Object a) {}

  void methodB(@Fenum("B") Object a) {}
}
