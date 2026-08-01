// Tests reduction of an equality constraint between two proper types (JLS 18.2.4): the constraint
// "reduces to true if S is the same as T (4.3.4), and false otherwise".  In the Checker Framework,
// the annotated types must additionally be comparable.
//
// The Value Checker is used because two of its qualifiers, such as @IntVal(1) and @IntVal(2), can
// be incomparable.  In a type system whose qualifiers are totally ordered, such as the Nullness
// Checker, the annotation part of this rule never rejects a constraint.

import java.util.List;
import java.util.Map;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.qual.UnknownVal;

public class ProperTypeEquality {

  <T> void takesMap(Map<@IntVal(1) Integer, T> p) {}

  <T> Map<@IntVal(1) Integer, T> returnsMap() {
    throw new RuntimeException();
  }

  <T> void takesArrayMap(Map<@IntVal(1) Integer[], T> p) {}

  <T> void takesWildcardMap(List<Map<? extends @IntVal(1) Integer, T>> p) {}

  // The argument constraint reduces to the equality constraint
  // <@IntVal(2) Integer = @IntVal(1) Integer> between two proper types.  The Java types are the
  // same, but the qualifiers are incomparable, so inference fails.
  void argumentConstraintIncomparable(Map<@IntVal(2) Integer, String> arg) {
    // :: error: [type.arguments.not.inferred]
    takesMap(arg);
  }

  // As above, but the proper types are array types.
  void argumentConstraintIncomparableArray(Map<@IntVal(2) Integer[], String> arg) {
    // :: error: [type.arguments.not.inferred]
    takesArrayMap(arg);
  }

  // The constraint between the method call's type and the target type reduces to the equality
  // constraint <@IntVal(1) Integer = @IntVal(2) Integer> between two proper types.
  void targetTypeConstraintIncomparable() {
    // :: error: [assignment] :: error: [type.arguments.not.inferred]
    Map<@IntVal(2) Integer, String> m = returnsMap();
  }

  // The qualifiers are the same, so inference succeeds.
  void sameQualifier(Map<@IntVal(1) Integer, String> arg) {
    takesMap(arg);
    Map<@IntVal(1) Integer, String> m = returnsMap();
  }

  // The qualifiers differ but are comparable, so inference succeeds; the invariance of the type
  // argument is diagnosed by the ordinary argument check rather than by inference.  Inference
  // requires the annotated types to be comparable rather than equal because it is deliberately
  // biased toward the annotations of the target type; see
  // AnnotatedTypeFactory#getDummyAssignedTo.
  void argumentConstraintComparable(Map<@UnknownVal Integer, String> arg) {
    // :: error: [argument]
    takesMap(arg);
  }

  // The equality constraint <? extends @IntVal(1) Integer = ? extends @IntVal(1) Integer> is
  // between two proper types that are wildcards.  Comparing them with
  // javax.lang.model.util.Types#isSameType, which returns false whenever either argument is a
  // wildcard, would make inference fail here.
  void argumentConstraintWildcard(List<Map<? extends @IntVal(1) Integer, String>> arg) {
    takesWildcardMap(arg);
  }
}
