// A method inherited from a raw superclass is erased no matter how it is invoked: through an
// implicit receiver, through `this`, or through `super`.  javac issues "unchecked call to
// put(Desc<String>) as a member of the raw type Super" for all three calls below, so the comment
// in DefaultTypeArgumentInference.outerInference that an implicit receiver "is `this`, which is
// never raw" does not hold.
//
// For the `this` and implicit-receiver forms, the receiver's type is the subclass, so
// TypesUtils.isRawCall does not find the member in it.  For the `super` form, javac's type for
// the receiver *is* the raw superclass, but the receiver type that AnnotatedTypes.asMemberOf
// sees is not raw, so the two disagree about whether the call is raw.

import org.checkerframework.checker.nullness.qual.Nullable;

@SuppressWarnings({"rawtypes", "unchecked"})
public class RawImplicitReceiver {

  interface Ser<T> {}

  static class Desc<T> {
    Desc(Ser<T> s) {}
  }

  static class Super<K> {
    void put(Desc<@Nullable String> d) {}

    <T> void generic(Desc<T> d) {}
  }

  static class Sub extends Super {

    void implicitReceiver(Desc<String> d) {
      // TODO: This is a false positive.  javac erases the inherited method's signature.
      // :: error: [argument]
      put(d);
    }

    void thisReceiver(Desc<String> d) {
      // TODO: This is a false positive.  javac erases the inherited method's signature.
      // :: error: [argument]
      this.put(d);
    }

    void superReceiver(Desc<String> d) {
      // TODO: This is a false positive.  javac erases the inherited method's signature.
      // :: error: [argument]
      super.put(d);
    }

    // TODO: Uncomment when the disagreement described above is fixed.  Today this crashes with
    // "StructuralEqualityComparer: unexpected combination:  type1: [TYPEVAR] T extends Object
    // type2: [DECLARED] String".  TreeUtils.isRawCall uses javac's type for `super`, which is the
    // raw superclass, and so skips outer inference for the diamond; but AnnotatedTypes.asMemberOf
    // uses a receiver type that is not raw and so does not erase the signature of `generic`,
    // whose type variable is then never instantiated.
    // void superReceiverWithDiamond(Ser<String> s) {
    //   super.generic(new Desc<>(s));
    // }
  }
}
