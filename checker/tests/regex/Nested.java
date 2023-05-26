import org.checkerframework.checker.regex.qual.Regex;

// TODO: @Regex is not allowed on arbitrary types. Find a better test case.
public class Nested {

  // :: error: (anno.on.irrelevant) :: warning: (cast.unsafe.constructor.invocation)
  OuterI.@Regex InnerA fa = new OuterI.@Regex InnerA() {};

  // :: error: (anno.on.irrelevant) :: warning: (cast.unsafe.constructor.invocation)
  OuterI.@Regex InnerB<Object> fb = new OuterI.@Regex InnerB<Object>() {};
}

class OuterI {
  static class InnerA {}

  static class InnerB<T> {}
}
