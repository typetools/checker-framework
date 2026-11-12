import java.util.ArrayList;
import java.util.Map;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.KeyFor;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

// A comparison of an assignment refines only the sides that have the assigned value afterward.
public class AssignmentInComparison {

  @Nullable AssignmentInComparison next;

  @Nullable AssignmentInComparison other;

  static @Nullable AssignmentInComparison global;

  @MonotonicNonNull AssignmentInComparison mono;

  static @MonotonicNonNull AssignmentInComparison monoGlobal;

  @Pure
  @Nullable AssignmentInComparison getNext() {
    return next;
  }

  @Pure
  static @Nullable AssignmentInComparison id(@Nullable AssignmentInComparison n) {
    return n;
  }

  static void localTarget(@Nullable AssignmentInComparison n) {
    if (n == null) {
      return;
    }
    if ((n = n.next) != null) {
      n.toString();
      // After the assignment, `n.next` is a field of a different object.
      // :: error: [dereference.of.nullable]
      n.next.toString();
    }
  }

  static void parenthesizedLocalTarget(@Nullable AssignmentInComparison n) {
    if (n == null) {
      return;
    }
    if (((n) = n.next) != null) {
      n.toString();
      // After the assignment, `n.next` is a field of a different object.
      // :: error: [dereference.of.nullable]
      n.next.toString();
    }
  }

  static void otherOperandReassignsReceiver(AssignmentInComparison n, AssignmentInComparison o) {
    if (n.next == (n = o)) {
      // The left operand is the `next` field of the old `n`, but `n.next` is now `o.next`.
      // :: error: [dereference.of.nullable]
      n.next.toString();
    }
  }

  static void assignmentInLeftOperand(
      @Nullable AssignmentInComparison n, AssignmentInComparison o) {
    if ((n = o) == n.next) {
      // The right operand is evaluated after the assignment.
      n.next.toString();
    }
  }

  static void localTargetFieldRhs(@Nullable AssignmentInComparison m, AssignmentInComparison n) {
    if ((m = n.next) != null) {
      m.toString();
      n.next.toString();
    }
  }

  static void fieldTarget(AssignmentInComparison n, AssignmentInComparison o) {
    if ((n.next = o.other) != null) {
      n.next.toString();
      // The assignment to `n.next` does not change `o.other`, which is a different field.
      o.other.toString();
    }
  }

  static void fieldTargetSameField(AssignmentInComparison n, AssignmentInComparison o) {
    if ((n.next = o.next) != null) {
      n.next.toString();
      // If `n` and `o` alias, the assignment stores into `o.next` the value it already has.
      o.next.toString();
    }
  }

  static void fieldTargetChangesRhsReceiver(AssignmentInComparison n, AssignmentInComparison o) {
    if (o.next == null) {
      return;
    }
    if ((n.next = o.next.other) != null) {
      n.next.toString();
      // The assignment to `n.next` might change `o.next`, if `n` and `o` alias.
      // :: error: [dereference.of.nullable]
      // :: error: [dereference.of.nullable]
      o.next.other.toString();
    }
  }

  static class Box<T> {
    @Nullable Box<?> next;
    @Nullable Box<?> other;
  }

  static void fieldTargetChangesRhsReceiverGeneric(
      Box<? extends Number> a, Box<? super Integer> b) {
    if (b.next == null) {
      return;
    }
    if ((a.next = b.next.other) != null) {
      a.next.toString();
      // Neither receiver type is a subtype of the other, but a `Box<Integer>` can be both. The
      // assignment to `a.next` might change `b.next`, if `a` and `b` alias.
      // :: error: [dereference.of.nullable]
      // :: error: [dereference.of.nullable]
      b.next.other.toString();
    }
  }

  static void fieldTargetMethodCallRhs(AssignmentInComparison n, AssignmentInComparison o) {
    if ((n.next = o.getNext()) != null) {
      n.next.toString();
      // The assignment to `n.next` might change the result of `o.getNext()`.
      // :: error: [dereference.of.nullable]
      o.getNext().toString();
    }
  }

  static void staticFieldRhs(AssignmentInComparison n) {
    if ((n.next = global) != null) {
      n.next.toString();
      global.toString();
    }
  }

  static void anonymousClassInitializerReassignsReceiver(AssignmentInComparison other) {
    if (global == null) {
      return;
    }
    if ((global.next =
            id(
                new AssignmentInComparison() {
                  {
                    global = other;
                  }
                }))
        != null) {
      // After the assignment, `global.next` is a field of a different object.
      // :: error: [dereference.of.nullable]
      global.next.toString();
    }
  }

  static void anonymousClassFieldInitializerReassignsReceiver(AssignmentInComparison other) {
    if (global == null) {
      return;
    }
    if ((global.next =
            id(
                new AssignmentInComparison() {
                  @Nullable Object f = (global = other);
                }))
        != null) {
      // After the assignment, `global.next` is a field of a different object.
      // :: error: [dereference.of.nullable]
      global.next.toString();
    }
  }

  // Sets `global` to `other` and returns a non-null value.
  static AssignmentInComparison reassignGlobal(AssignmentInComparison other) {
    global = other;
    return other;
  }

  static void methodCallReassignsGlobalReceiver(AssignmentInComparison other) {
    if (global == null) {
      return;
    }
    // :: warning: [nulltest.redundant]
    if ((global.next = reassignGlobal(other)) != null) {
      // After the assignment, `global.next` is a field of a different object.
      // :: error: [dereference.of.nullable]
      global.next.toString();
    }
  }

  static void methodCallReassignsReceiverNoComparison(AssignmentInComparison other) {
    if (global == null) {
      return;
    }
    global.next = reassignGlobal(other);
    // After the assignment, `global.next` is a field of a different object.
    // :: error: [dereference.of.nullable]
    global.next.toString();
  }

  // A method call cannot reassign a local receiver.
  static void methodCallLocalReceiver(AssignmentInComparison n, AssignmentInComparison other) {
    // :: warning: [nulltest.redundant]
    if ((n.next = reassignGlobal(other)) != null) {
      n.next.toString();
    }
  }

  static void anonymousClassInitializerCallsMethod(AssignmentInComparison replacement) {
    if (global == null) {
      return;
    }
    if ((global.next =
            id(
                new AssignmentInComparison() {
                  {
                    helper(replacement);
                  }

                  static void helper(AssignmentInComparison r) {
                    global = r;
                  }
                }))
        != null) {
      // After the assignment, `global.next` is a field of a different object.
      // :: error: [dereference.of.nullable]
      global.next.toString();
    }
  }

  @Nullable AssignmentInComparison advance(AssignmentInComparison o) {
    mono = new AssignmentInComparison();
    return o;
  }

  void methodCallReassignsReceiver(AssignmentInComparison o) {
    if (mono == null) {
      return;
    }
    if ((mono.next = advance(o)) != null) {
      // After the assignment, `mono.next` is a field of a different object.
      // :: error: [dereference.of.nullable]
      mono.next.toString();
    }
  }

  static <T> @Nullable T call(Supplier<T> s) {
    return s.get();
  }

  static void lambdaReassignsReceiver(AssignmentInComparison other, AssignmentInComparison o) {
    if (monoGlobal == null) {
      return;
    }
    if ((monoGlobal.next =
            call(
                () -> {
                  monoGlobal = other;
                  return o;
                }))
        != null) {
      // After the assignment, `monoGlobal.next` is a field of a different object.
      // :: error: [dereference.of.nullable]
      monoGlobal.next.toString();
    }
  }

  void chainOfFieldTargets(@Nullable AssignmentInComparison o) {
    if ((this.next = this.other = o) != null) {
      this.next.toString();
      this.other.toString();
      o.toString();
    }
  }

  static void arrayTarget(@Nullable Object[] a, @Nullable Object[] b) {
    if ((a[0] = b[1]) != null) {
      a[0].toString();
      // If `a` and `b` alias, the assignment stores into `b[1]` the value it already has.
      b[1].toString();
    }
  }

  static void fieldTargetLocalRhs(AssignmentInComparison n, @Nullable AssignmentInComparison o) {
    if ((n.next = o) != null) {
      n.next.toString();
      o.toString();
    }
  }

  static void receiverReassigned(
      @Nullable AssignmentInComparison n, @Nullable AssignmentInComparison o) {
    if (n == null) {
      return;
    }
    if ((n.next = (n = o)) != null) {
      n.toString();
      o.toString();
      // After the assignment, `n.next` is a field of a different object.
      // :: error: [dereference.of.nullable]
      n.next.toString();
    }
  }

  static void receiverReassignedParenthesized(
      @Nullable AssignmentInComparison n, @Nullable AssignmentInComparison o) {
    if (n == null) {
      return;
    }
    if ((n.next = ((n) = o)) != null) {
      n.toString();
      o.toString();
      // After the assignment, `n.next` is a field of a different object.
      // :: error: [dereference.of.nullable]
      n.next.toString();
    }
  }

  static void receiverReassignedInRhsReceiver(
      @Nullable AssignmentInComparison n, AssignmentInComparison o) {
    if (n == null) {
      return;
    }
    if ((n.next = (n = o).other) != null) {
      // After the assignment, `n.next` is a field of a different object.
      // :: error: [dereference.of.nullable]
      n.next.toString();
    }
  }

  static void receiverReassignedInArgument(
      @Nullable AssignmentInComparison n, AssignmentInComparison o) {
    if (n == null) {
      return;
    }
    if ((n.next = id(n = o)) != null) {
      // After the assignment, `n.next` is a field of a different object.
      // :: error: [dereference.of.nullable]
      n.next.toString();
    }
  }

  static void indexIncrementedInRhs(@Nullable Object[] a, int i, Object o) {
    if ((a[i] = id2(i++, o)) != null) {
      // After the assignment, `a[i]` is a different element of `a`.
      // :: error: [dereference.of.nullable]
      a[i].toString();
    }
  }

  static void arrayTargetWithAssignmentInIndex(@Nullable Object[] a, int i, @Nullable Object o) {
    if ((a[i = 0] = o) != null) {
      // The assignment to an element of `a` does not change the local variable `o`.
      o.toString();
    }
  }

  static void assignmentInTargetIndexBeforeRhs(@Nullable Object[] a, @Nullable Object[] b, int i) {
    if ((a[i = 0] = b[i]) != null) {
      // `b[i]` is evaluated after `i = 0`, so it has the assigned value.
      b[i].toString();
    }
  }

  static void fieldTargetWithAssignmentInReceiver(
      AssignmentInComparison n, AssignmentInComparison m, @Nullable AssignmentInComparison o) {
    if (((n = m).next = o) != null) {
      // The assignment to a `next` field does not change the local variable `o`.
      o.toString();
    }
  }

  @Pure
  static @Nullable Object id2(int i, @Nullable Object o) {
    return o;
  }

  @SuppressWarnings("unchecked") // cast to T
  static <T> void arrayTargetTypeVariable(@Nullable T[] ts, @Nullable String[][] ss) {
    if ((ts[0] = (T) ss[0][0]) != null) {
      ts[0].toString();
      // The assignment to `ts[0]` might change `ss[0]`, if `ts` and `ss` alias.
      // :: error: [dereference.of.nullable]
      ss[0][0].toString();
    }
  }

  static Object nonNull(@Nullable Object o) {
    return "";
  }

  // The right operand assigns `x` after the left operand reads it, so the comparison says nothing
  // about the new value of `x`.
  void assignmentInRightOperand(@Nullable Object x) {
    if (x == nonNull(x = null)) {
      // :: error: [dereference.of.nullable]
      x.toString();
    }
  }

  void assignmentInParenthesizedRightOperand(@Nullable Object x) {
    if ((x) == (nonNull(x = null))) {
      // :: error: [dereference.of.nullable]
      x.toString();
    }
  }

  void fieldAssignmentInRightOperand(AssignmentInComparison n) {
    if (n.next == nonNull(n.next = null)) {
      // :: error: [dereference.of.nullable]
      n.next.toString();
    }
  }

  void aliasedFieldAssignmentInRightOperand(AssignmentInComparison n, AssignmentInComparison o) {
    // If `n` and `o` alias, the right operand assigns `n.next`.
    if (n.next == nonNull(o.next = null)) {
      // :: error: [dereference.of.nullable]
      n.next.toString();
    }
  }

  void aliasedArrayAssignmentInRightOperand(@Nullable Object[] a, @Nullable Object[] b) {
    // If `a` and `b` alias, the right operand assigns `a[0]`.
    if (a[0] == nonNull(b[0] = null)) {
      // :: error: [dereference.of.nullable]
      a[0].toString();
    }
  }

  // The assignment writes the `next` field of the original `n`, not of `o`.
  static void receiverReassignedNonNullRhs(AssignmentInComparison n, AssignmentInComparison o) {
    // :: warning: [nulltest.redundant]
    if ((n.next = (n = o)) != null) {
      // :: error: [dereference.of.nullable]
      n.next.toString();
    }
  }

  static void receiverReassignedNoComparison(AssignmentInComparison n, AssignmentInComparison o) {
    n.next = (n = o);
    // :: error: [dereference.of.nullable]
    n.next.toString();
  }

  static AssignmentInComparison nonNullGlobal = new AssignmentInComparison();

  // Sets `nonNullGlobal` to `n` and returns a non-null value.
  static AssignmentInComparison reassignNonNullGlobal(AssignmentInComparison n) {
    nonNullGlobal = n;
    return new AssignmentInComparison();
  }

  // The call reassigns `nonNullGlobal` after the receiver of the target is evaluated, so the
  // assignment writes the `next` field of the original `nonNullGlobal`.
  static void receiverReassignedByCall(AssignmentInComparison o) {
    nonNullGlobal.next = reassignNonNullGlobal(o);
    // :: error: [dereference.of.nullable]
    nonNullGlobal.next.toString();
  }

  static void receiverReassignedByCallInComparison(AssignmentInComparison o) {
    // :: warning: [nulltest.redundant]
    if ((nonNullGlobal.next = reassignNonNullGlobal(o)) != null) {
      // :: error: [dereference.of.nullable]
      nonNullGlobal.next.toString();
    }
  }

  // A call cannot reassign a local variable, so it cannot change the location of `n.next`.
  static void localReceiverWithCall(AssignmentInComparison n, AssignmentInComparison o) {
    n.next = reassignNonNullGlobal(o);
    n.next.toString();
  }

  // The left operand assigns `x` before the right operand reads it, so refining `x` is sound.
  void assignmentInLeftOperand(@Nullable Object x) {
    if (nonNull(x = null) == x) {
      x.toString();
    }
  }

  @Nullable String f;

  // Sets `f` to null and returns its old value, or a non-null value.
  String reset() {
    String result = (f == null) ? "" : f;
    f = null;
    return result;
  }

  @Pure
  String constant() {
    return "";
  }

  // The right operand calls a method that might change `f` after the left operand reads it.
  void methodWithSideEffectsInRightOperand() {
    if (f == reset()) {
      // :: error: [dereference.of.nullable]
      f.toString();
    }
  }

  // A method call cannot change a local variable.
  void methodWithSideEffectsInRightOperandLocal(@Nullable String s) {
    if (s == reset()) {
      s.toString();
    }
  }

  // A side-effect-free call in the right operand does not change `f`.
  void sideEffectFreeCallInRightOperand() {
    if (f == constant()) {
      f.toString();
    }
  }

  // Java assigns `a[0]`, but after the assignment `a[i]` is `a[1]`.
  static void indexPostfixIncrementedRhs(@Nullable Integer[] a) {
    int i = 0;
    a[i] = i++;
    // :: error: [dereference.of.nullable]
    a[i].intValue();
  }

  static class Holder {
    @Nullable String value;
    final String text = "";
  }

  // The assignment stores a non-null value, so it cannot make `p.value` null even if `p` aliases
  // the original `n`.
  static void receiverReassignedPreservesOtherRefinements(Holder p, Holder n, Holder o) {
    if (p.value != null) {
      n.value = (n = o).text;
      p.value.length();
    }
  }

  static void annotationArgumentInRhs(
      @Nullable Object o, @Nullable Object p, Map<String, Object> m) {
    // The annotation argument `"m"` is not an assignment to a variable.
    if ((o = new ArrayList<@KeyFor("m") String>()) == p) {
      p.toString();
    }
  }
}
