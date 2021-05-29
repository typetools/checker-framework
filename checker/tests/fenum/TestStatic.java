import org.checkerframework.checker.fenum.qual.Fenum;

@SuppressWarnings("fenum:assignment")
public class TestStatic {
  public static final @Fenum("A") int ACONST1 = 1;
  public static final @Fenum("A") int ACONST2 = 2;
  public static final @Fenum("A") int ACONST3 = 3;

  public static final @Fenum("B") int BCONST1 = 4;
  public static final @Fenum("B") int BCONST2 = 5;
  public static final @Fenum("B") int BCONST3 = 6;
}

class FenumUserTestStatic {
  @Fenum("A") int state1 = TestStatic.ACONST1;

  // :: error: (assignment)
  @Fenum("B") int state2 = TestStatic.ACONST1;

  void bar(@Fenum("A") int p) {}

  void foo() {
    // :: error: (assignment)
    state1 = 4;

    state1 = TestStatic.ACONST2;
    state1 = TestStatic.ACONST3;

    state2 = TestStatic.BCONST3;

    // :: error: (assignment)
    state1 = TestStatic.BCONST1;

    // :: error: (argument)
    bar(5);
    bar(TestStatic.ACONST1);
    // :: error: (argument)
    bar(TestStatic.BCONST1);
  }

  @SuppressWarnings("fenum")
  void ignoreAll() {
    state1 = 4;
    bar(5);
  }

  @SuppressWarnings("fenum:assignment")
  void ignoreOne() {
    state1 = 4;
    // :: error: (argument)
    bar(5);
  }
}
