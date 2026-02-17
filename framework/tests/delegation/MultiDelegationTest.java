import org.checkerframework.common.delegation.qual.*;

class MultiDelegationTest {

  @Delegate public int foo;

  // :: error: (multiple.delegate.annotations)
  @Delegate public int bar;
}
