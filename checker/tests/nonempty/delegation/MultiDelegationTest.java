import org.checkerframework.checker.nonempty.qual.Delegate;

class MultiDelegationTest {

  @Delegate public int foo;

  // :: error: (multiple.delegate.annotations)
  @Delegate public int bar;
}
