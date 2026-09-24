import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

// A use of a type variable is relevant only if the type variable's upper bound is relevant.
// `Number` is not listed in the checker's `@RelevantJavaTypes` and is not related to a listed type
// by subtyping, so inference must not write, into the .ajava file, an annotation that it infers
// for a use of `T`.  The checker rejects the annotation that is written below, for the same
// reason, in both passes of this test.
public class IrrelevantTypeVariable<T extends Number> {

  T field;

  @SuppressWarnings("anno.on.irrelevant") // intentional:  `T`'s upper bound is irrelevant
  void assignField(@AinferSibling1 T t) {
    field = t;
  }
}

// Likewise when the upper bound is an intersection type.  Its erasure is its leftmost bound,
// `Number`, which is not relevant.
class IrrelevantIntersectionTypeVariable<T extends Number & java.io.Serializable> {

  T field;

  @SuppressWarnings("anno.on.irrelevant") // intentional:  `T`'s upper bound is irrelevant
  void assignField(@AinferSibling1 T t) {
    field = t;
  }
}

// Likewise when the upper bound is another type variable, whose upper bound is not relevant.
class IrrelevantTypeVariableBoundedByTypeVariable<U extends Number, T extends U> {

  T field;

  @SuppressWarnings("anno.on.irrelevant") // intentional:  `T`'s upper bound is irrelevant
  void assignField(@AinferSibling1 T t) {
    field = t;
  }
}

// Likewise when a use of the type variable is the element type of an array type.
class IrrelevantTypeVariableArray<T extends Number> {

  T[] field;

  @SuppressWarnings("anno.on.irrelevant") // intentional:  `T`'s upper bound is irrelevant
  void assignField(@AinferSibling1 T[] t) {
    field = t;
  }
}
