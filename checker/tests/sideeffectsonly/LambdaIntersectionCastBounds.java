// A lambda that is cast to an intersection type can run through a method of any bound of the
// intersection, or of a supertype of a bound, that is override-equivalent to the functional
// interface method as a member of that bound.  Such a method must be found using the type
// arguments that javac attributes to the bound.

import java.util.ArrayList;
import java.util.Collection;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class LambdaIntersectionCastBounds {

  static Collection<Integer> other = new ArrayList<>();

  int field;

  // `A.run` is override-equivalent to `B.run` only as a member of `C<String>`, not as a member of
  // `C<T>`.  javac chooses `B.run`, whose return type is the most specific, as the functional
  // interface method of `D & C<String>`.

  interface A<T> {
    @SideEffectFree
    Object run(T t);
  }

  interface B {
    CharSequence run(String s);
  }

  interface C<T> extends A<T>, B {}

  interface D {
    Object run(String s);
  }

  void sharedSupertype() {
    Object o =
        (D & C<String>)
            x -> {
              // :: error: (purity.assign.field)
              field = 1;
              return "";
            };
  }

  // javac instantiates `E<? super String>` as `E<CharSequence>`, the explicitly declared type of
  // the lambda's parameter.  `E.run` is override-equivalent to `F.run` only as a member of
  // `E<CharSequence>`, not as a member of `E<String>`.

  interface E<T> {
    Object run(T t);
  }

  interface F {
    @SideEffectsOnly("#1")
    Object run(CharSequence s);
  }

  void explicitlyTypedLambda() {
    // :: error: (purity.incorrect.sideeffectsonly)
    Object o = (E<? super String> & F) (CharSequence x) -> other.add(1);
  }
}
