import checkers.fenum.quals.*;

@SuppressWarnings("fenum:assignment.type.incompatible")
public class TestStatic {
  public static final @Fenum("A") int ACONST1 = 1;
  public static final @Fenum("A") int ACONST2 = 2;
  public static final @Fenum("A") int ACONST3 = 3;

  public static final @Fenum("B") int BCONST1 = 4;
  public static final @Fenum("B") int BCONST2 = 5;
  public static final @Fenum("B") int BCONST3 = 6;
}

class FenumUser {
  @Fenum("A")  int state1 = TestStatic.ACONST1;

  //:: error: (assignment.type.incompatible)
  @Fenum("B")  int state2 = TestStatic.ACONST1;

  void bar(@Fenum("A") int p) {
  }

  void foo() {
    //:: error: (assignment.type.incompatible)
    state1 = 4;

    state1 = TestStatic.ACONST2;
    state1 = TestStatic.ACONST3;

    state2 = TestStatic.BCONST3;

    //:: error: (assignment.type.incompatible)
    state1 = TestStatic.BCONST1;

    //:: error: (argument.type.incompatible)
    bar(5);
    bar(TestStatic.ACONST1);
    //:: error: (argument.type.incompatible)
    bar(TestStatic.BCONST1);
  }

  @SuppressWarnings("fenum")
  void ignoreAll() {
    state1 = 4;
    bar(5);
  }

  @SuppressWarnings("fenum:assignment.type.incompatible")
  void ignoreOne() {
    state1 = 4;
    //:: error: (argument.type.incompatible)
    bar(5);
  }
}
