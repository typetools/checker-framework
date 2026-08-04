// Test that two ProperTypes that stand for the same Java type are equal, even when they were
// created from different TypeMirrors.  The checking is done by
// org.checkerframework.framework.testchecker.typeinference8.ProperTypeChecker.

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ProperTypeEquality {

  String getFromList(List<String> list) {
    // The declared return type of List.get is the type variable E.
    return list.get(0);
  }

  List<String> emptyList() {
    // The declared return type of Collections.emptyList is the type variable T.
    return Collections.emptyList();
  }

  <T> T identity(T t) {
    return t;
  }

  Number useGenericMethod() {
    // The declared return type of identity is the type variable T.
    return identity(Integer.valueOf(1));
  }

  List<List<String>> nested() {
    List<List<String>> result = new ArrayList<>();
    result.add(Collections.emptyList());
    return result;
  }

  // Capture conversion: the receiver's type argument is a wildcard, so resolution goes through
  // Resolution#resolveWithCapture and InferenceFactory#getSubsTypeArgs.
  Number capture(List<? extends Number> list) {
    return list.get(0);
  }

  List<? extends Number> nestedCapture(List<List<? extends Number>> lists) {
    return lists.get(0);
  }

  // A type variable with a bound, which exercises AbstractType#getTypeParameterBounds.
  <T extends Comparable<T>> T max(List<T> list) {
    return Collections.max(list);
  }

  String useBoundedTypeVariable(List<String> list) {
    return max(list);
  }

  <T extends Number & Comparable<T>> T intersectionBound(T t) {
    return t;
  }

  Integer useIntersectionBound(Integer i) {
    return intersectionBound(i);
  }

  // Boxing: the inferred type argument is a boxed primitive, which exercises ProperType#boxType.
  int boxing() {
    return identity(1);
  }

  double[] arrays(double[] array) {
    // The declared return type of identity is the type variable T, instantiated to an array type.
    return identity(array);
  }

  // A varargs call whose arguments are themselves generic method invocations, which exercises the
  // varargs branch of InferenceFactory#assignedToExecutable.
  List<String> varargs() {
    return Arrays.asList(identity("a"), identity("b"));
  }

  // Instantiation of a generic class, which the checker inspects via visitNewClass.
  List<String> newGenericClass() {
    return new ArrayList<String>();
  }

  List<String> diamond() {
    return new ArrayList<>();
  }

  // Nested generic method invocations, so that an inference variable's instantiation mentions
  // another inference variable.
  List<List<String>> nestedInference() {
    return identity(new ArrayList<List<String>>());
  }
}
