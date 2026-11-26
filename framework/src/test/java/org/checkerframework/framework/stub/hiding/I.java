package org.checkerframework.framework.stub.hiding;

/** An interface with a member type. Used by {@code ToIndexFileConverterTest}. */
public interface I {
  /** A member type, which is implicitly public. */
  class N {
    /** A private member type, which inherits the member types of {@link Base}. */
    @SuppressWarnings("UnusedNestedClass")
    private static class P extends Base {}

    /** A member type whose private member type hides {@link Base.M}. */
    public static class Q extends Base {
      /** A private member type, which hides {@link Base.M}. */
      @SuppressWarnings("UnusedNestedClass")
      private static class M {}
    }
  }
}
