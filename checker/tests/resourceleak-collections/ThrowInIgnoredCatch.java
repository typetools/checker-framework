/*
 * Tests throwing a non-collection local in a catch block that the Collection Ownership analysis
 * does not reach, because every exception that leads to it is an ignored exception type. The
 * local has no dataflow value there, so its declared type is checked against the thrown-type
 * bound.
 */
class ThrowInIgnoredCatch {

  static void work() {}

  void catchThrowable() {
    try {
      work();
    } catch (Throwable t) {
      RuntimeException re = new RuntimeException("failed", t);
      re.printStackTrace();
      throw re;
    }
  }

  void catchRuntimeException() {
    try {
      work();
    } catch (RuntimeException t) {
      RuntimeException re = new RuntimeException("failed", t);
      throw re;
    }
  }

  void throwOutsideCatch() {
    RuntimeException re = new RuntimeException("failed");
    throw re;
  }
}
