import org.checkerframework.checker.fenum.qual.Fenum;

@SuppressWarnings("fenum:assignment")
public class TestPrimitive {
  public final @Fenum("A") int ACONST1 = 1;
  public final @Fenum("A") int ACONST2 = 2;
  public final @Fenum("A") int ACONST3 = 3;

  public final @Fenum("B") int BCONST1 = 4;
  public final @Fenum("B") int BCONST2 = 5;
  public final @Fenum("B") int BCONST3 = 6;
}

class FenumUserTestPrimitive {
  @Fenum("A") int state1 = new TestPrimitive().ACONST1;

  @Fenum("A") int state3 = this.state1;

  // :: error: (assignment)
  @Fenum("B") int state2 = new TestPrimitive().ACONST1;

  void foo(TestPrimitive t) {
    // :: error: (assignment)
    state1 = 4;

    state1 = t.ACONST2;
    state1 = t.ACONST3;

    // :: error: (assignment)
    state1 = t.BCONST1;

    if (t.ACONST1 < t.ACONST2) {
      // ok
    }

    // :: error: (binary)
    if (t.ACONST1 < t.BCONST2) {}
    // :: error: (binary)
    if (t.ACONST1 == t.BCONST2) {}

    // :: error: (binary)
    if (t.ACONST1 < 5) {}
    // :: error: (binary)
    if (t.ACONST1 == 5) {}
  }
}
