public class WildcardCharPrimitive {
  static interface Predicate<T> {
    boolean apply(T t);
  }

  abstract static class Matcher {

    public abstract boolean matches(char character);
  }

  public static void forPredicate(final Predicate<? super Character> predicate) {

    new Matcher() {
      //  this tests default type hierarchy visitPrimitive_Wildcard
      public boolean matches(char c) {
        //  this will happen when a type system does not have an EXPLICIT_LOWER_BOUND
        //  that matches the default for char c
        @SuppressWarnings("argument.type.incompatible")
        boolean value = predicate.apply(c);
        return value;
      }
    };
  }
}
