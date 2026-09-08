import org.checkerframework.common.delegation.qual.*;

class MultiDelegationTest {

  // :: error: [multiple.delegate.annotations]
  @Delegate public int foo;

  // :: error: [multiple.delegate.annotations]
  @Delegate public int bar;
}
