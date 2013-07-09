public class PrivateMethodUnknownInit {

  int x;

  public PrivateMethodUnknownInit() {
    x = 1;
    // This call is OK because the method is private.  A private method's
    // receiver should be implicitly not @Initialized, but
    // @UnknownInitialization(thisclass).  Thus, it is safe to call from
    // the end of the constructor, which is OK because a private method can
    // never be called from outside the class itself.
    m1();
    //:: error: (method.invocation.invalid)
    m2();
  }

  private void m1() { }

  public void m2() { }

}
