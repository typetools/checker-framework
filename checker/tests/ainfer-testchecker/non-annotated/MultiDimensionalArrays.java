// This test ensures that annotations on different component types of multidimensional arrays
// are printed correctly.

import java.util.List;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling2;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSiblingWithFields;
import org.checkerframework.common.aliasing.qual.MaybeAliased;
import org.checkerframework.common.aliasing.qual.NonLeaked;
import org.checkerframework.common.aliasing.qual.Unique;

public class MultiDimensionalArrays {

  // two dimensional arrays

  void requiresS1S2(@AinferSibling1 int @AinferSibling2 [] x) {}

  int[] twoDimArray;

  void testField() {
    // :: warning: (argument)
    requiresS1S2(twoDimArray);
  }

  void useField(@AinferSibling1 int @AinferSibling2 [] x) {
    twoDimArray = x;
  }

  void testParam(int[] x) {
    // :: warning: (argument)
    requiresS1S2(x);
  }

  void useParam(@AinferSibling1 int @AinferSibling2 [] x) {
    testParam(x);
  }

  int[] useReturn(@AinferSibling1 int @AinferSibling2 [] x) {
    return x;
  }

  void testReturn() {
    requiresS1S2(
        // :: warning: (argument)
        useReturn(
            // :: warning: (argument)
            twoDimArray));
  }

  // three dimensional arrays

  void requiresS1S2S1(@AinferSibling1 int @AinferSibling2 [] @AinferSibling1 [] x) {}

  int[][] threeDimArray;

  void testField2() {
    // :: warning: (argument)
    requiresS1S2S1(threeDimArray);
  }

  void useField2(@AinferSibling1 int @AinferSibling2 [] @AinferSibling1 [] x) {
    threeDimArray = x;
  }

  void testParam2(int[][] x) {
    // :: warning: (argument)
    requiresS1S2S1(x);
  }

  void useParam2(@AinferSibling1 int @AinferSibling2 [] @AinferSibling1 [] x) {
    testParam2(x);
  }

  int[][] useReturn2(@AinferSibling1 int @AinferSibling2 [] @AinferSibling1 [] x) {
    return x;
  }

  void testReturn2() {
    // :: warning: (argument)
    requiresS1S2S1(useReturn2(threeDimArray));
  }

  // three dimensional array with annotations only on two inner types

  void requiresS1S2N(@AinferSibling1 int @AinferSibling2 [][] x) {}

  int[][] threeDimArray2;

  void testField3() {
    // :: warning: (argument)
    requiresS1S2N(threeDimArray2);
  }

  void useField3(@AinferSibling1 int @AinferSibling2 [][] x) {
    threeDimArray2 = x;
  }

  void testParam3(int[][] x) {
    // :: warning: (argument)
    requiresS1S2N(x);
  }

  void useParam3(@AinferSibling1 int @AinferSibling2 [][] x) {
    testParam3(x);
  }

  int[][] useReturn3(@AinferSibling1 int @AinferSibling2 [][] x) {
    return x;
  }

  void testReturn3() {
    // :: warning: (argument)
    requiresS1S2N(useReturn3(threeDimArray2));
  }

  // three dimensional array with annotations only on two array types, not innermost type

  void requiresS2S1(int @AinferSibling2 [] @AinferSibling1 [] x) {}

  int[][] threeDimArray3;

  void testField4() {
    // :: warning: (argument)
    requiresS2S1(threeDimArray3);
  }

  void useField4(int @AinferSibling2 [] @AinferSibling1 [] x) {
    threeDimArray3 = x;
  }

  void testParam4(int[][] x) {
    // :: warning: (argument)
    requiresS2S1(x);
  }

  void useParam4(int @AinferSibling2 [] @AinferSibling1 [] x) {
    testParam4(x);
  }

  int[][] useReturn4(int @AinferSibling2 [] @AinferSibling1 [] x) {
    return x;
  }

  void testReturn4() {
    // :: warning: (argument)
    requiresS2S1(useReturn4(threeDimArray3));
  }

  // three-dimensional arrays with arguments in annotations

  void requiresSf1Sf2Sf3(
      @AinferSiblingWithFields(value = {"test1", "test1"}) int @AinferSiblingWithFields(value = {"test2", "test2"}) []
                  @AinferSiblingWithFields(value = {"test3"}) []
              x) {}

  int[][] threeDimArray4;

  void testField5() {
    // :: warning: (argument)
    requiresSf1Sf2Sf3(threeDimArray4);
  }

  void useField5(
      @AinferSiblingWithFields(value = {"test1", "test1"}) int @AinferSiblingWithFields(value = {"test2", "test2"}) []
                  @AinferSiblingWithFields(value = {"test3"}) []
              x) {
    threeDimArray4 = x;
  }

  void testParam5(int[][] x) {
    // :: warning: (argument)
    requiresSf1Sf2Sf3(x);
  }

  void useParam5(
      @AinferSiblingWithFields(value = {"test1", "test1"}) int @AinferSiblingWithFields(value = {"test2", "test2"}) []
                  @AinferSiblingWithFields(value = {"test3"}) []
              x) {
    testParam5(x);
  }

  int[][] useReturn5(
      @AinferSiblingWithFields(value = {"test1", "test1"}) int @AinferSiblingWithFields(value = {"test2", "test2"}) []
                  @AinferSiblingWithFields(value = {"test3"}) []
              x) {
    return x;
  }

  void testReturn5() {
    // :: warning: (argument)
    requiresSf1Sf2Sf3(useReturn5(threeDimArray4));
  }

  // three dimensional array with annotations from other hierarchies that ought to be preserved

  int[][] threeDimArray5;

  void testField6() {
    // :: warning: (argument)
    requiresS1S2S1(threeDimArray5);
  }

  void useField6(
      @AinferSibling1 @Unique int @AinferSibling2 @NonLeaked [] @AinferSibling1 @MaybeAliased [] x) {
    threeDimArray5 = x;
  }

  void testParam6(int[][] x) {
    // :: warning: (argument)
    requiresS1S2S1(x);
  }

  void useParam6(
      @AinferSibling1 @Unique int @AinferSibling2 @NonLeaked [] @AinferSibling1 @MaybeAliased [] x) {
    testParam6(x);
  }

  int[][] useReturn6(
      @AinferSibling1 @Unique int @AinferSibling2 @NonLeaked [] @AinferSibling1 @MaybeAliased [] x) {
    return x;
  }

  void testReturn6() {
    // :: warning: (argument)
    requiresS1S2S1(useReturn6(threeDimArray));
  }

  // Shenanigans with lists + arrays; commented out annotations can't be inferred by either
  // jaif or stub based WPI for now due to limitations in generics inference.

  List<String[]>[] arrayofListsOfStringArrays;

  void testField7() {
    // :: warning: (argument)
    requiresS1S2L(arrayofListsOfStringArrays);
  }

  void requiresS1S2L(
      @AinferSibling1 List</*@AinferSibling1*/ String /*@AinferSibling2*/[]> @AinferSibling2 [] la) {}

  void useField7(
      @AinferSibling1 List</*@AinferSibling1*/ String /*@AinferSibling2*/[]> @AinferSibling2 [] x) {
    arrayofListsOfStringArrays = x;
  }

  void testParam7(List<String[]>[] x) {
    // :: warning: (argument)
    requiresS1S2L(x);
  }

  void useParam7(
      @AinferSibling1 List</*@AinferSibling1*/ String /*@AinferSibling2*/[]> @AinferSibling2 [] x) {
    testParam7(x);
  }

  List<String[]>[] useReturn7(
      @AinferSibling1 List</*@AinferSibling1*/ String /*@AinferSibling2*/[]> @AinferSibling2 [] x) {
    return x;
  }

  void testReturn7() {
    // :: warning: (argument)
    requiresS1S2L(useReturn7(arrayofListsOfStringArrays));
  }
}
