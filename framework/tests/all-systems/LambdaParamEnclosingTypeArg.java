// The type of the implicitly typed lambda parameter is `Outer<A>.Inner`, where `A` is a type
// variable declared by a method.  `A` is mentioned only in the enclosing type: the type arguments
// of `Inner` itself are empty.  Such a type is not this inference problem's to supply as the
// parameter's type, so InvocationTypeInference.getLambdaParameterType must decline to, which it
// does only if it searches the enclosing type as well as the type arguments.

import java.util.function.Function;

public class LambdaParamEnclosingTypeArg {

  static class Outer<T> {
    class Inner {}
  }

  static class Box<T, U> {}

  static <T, U> Box<T, U> reduce(U identity, Function<T, U> mapper) {
    throw new AssertionError();
  }

  static <A> Box<Outer<A>.Inner, String> m() {
    return reduce("", i -> i.toString());
  }
}
