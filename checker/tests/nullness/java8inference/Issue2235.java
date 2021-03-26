// Test case that was submitted in Issue 2235, but is caused
// by a false negative from Issue 979
// https://github.com/typetools/checker-framework/issues/979

// Skip until correct error is issued.
// @skip-test

public class Issue2235 {
  // Simple wrapper class with a public generic method
  // to make an instance:
  static class Holder<T> {
    T t;

    private Holder(T t) {
      this.t = t;
    }

    public static <T> Holder<T> make(T t) {
      return new Holder<>(t);
    }
  }

  public static void main(String[] args) throws Exception {
    // Null is hidden via nested calls, but assigned to a non-null type:
    // :: error: (TODO)
    Holder<Holder<String>> h = Holder.make(Holder.make(null));
    // NullPointerException will fire here:
    h.t.t.toString();
  }
}
