// Regression test for an array dimension whose type is a type variable with a boxed-primitive
// bound.  It is in its own file so that a crash elsewhere does not mask a crash here.
public class TypeVarArrayDimension {
  <T extends Integer> int[] arrayDimension(T t) {
    return new int[t];
  }
}
