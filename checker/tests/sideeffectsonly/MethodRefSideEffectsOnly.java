// A method reference must satisfy the `@SideEffectsOnly` annotation of the functional interface
// method it implements. That check reports `purity.methodref` rather than `purity.overriding`.

import java.util.Collection;
import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class MethodRefSideEffectsOnly {

  interface Mutator {
    @SideEffectsOnly("#1")
    void apply(Collection<Integer> c);
  }

  @SideEffectsOnly("#1")
  static void mutatesArgument(Collection<Integer> c) {
    c.add(1);
  }

  @SideEffectFree
  static void mutatesNothing(Collection<Integer> c) {}

  static void unannotated(Collection<Integer> c) {}

  void ok() {
    Mutator m1 = MethodRefSideEffectsOnly::mutatesArgument;
    // `@SideEffectFree` is a stronger guarantee than any `@SideEffectsOnly`.
    Mutator m2 = MethodRefSideEffectsOnly::mutatesNothing;
  }

  void notOk() {
    // The referenced method promises nothing, so it cannot implement a `@SideEffectsOnly` method.
    // :: error: (purity.methodref)
    Mutator m = MethodRefSideEffectsOnly::unannotated;
  }

  interface DeterministicMutator {
    @Deterministic
    @SideEffectsOnly("#1")
    int apply(Collection<Integer> c);
  }

  @SideEffectsOnly("#1")
  static int mutatesArgumentNondeterministically(Collection<Integer> c) {
    c.add(1);
    return 0;
  }

  void determinismNotOk() {
    // The referenced method satisfies the `@SideEffectsOnly` annotation, but not the
    // `@Deterministic` one.  (Unlike an overriding method, a referenced method does not inherit
    // the annotations of the method that it implements.)
    // :: error: (purity.methodref)
    DeterministicMutator m = MethodRefSideEffectsOnly::mutatesArgumentNondeterministically;
  }

  // For an unbound method reference (`Type::instanceMethod`), the interface method's first
  // parameter is the referenced method's receiver, and the interface method's parameter `#(i+1)`
  // is the referenced method's parameter `#i`.  The two annotations must be compared with that
  // shift.

  void unboundOk() {
    // `Collection.clear()` is `@SideEffectsOnly("this")` in the annotated JDK.  Its receiver is
    // `Mutator.apply`'s `#1`, which `Mutator.apply`'s annotation lists.
    Mutator m = Collection<Integer>::clear;
  }

  static class Cell {
    Collection<Integer> coll = new java.util.ArrayList<>();

    @SideEffectsOnly("this.coll")
    void mutatesOwnField(Collection<Integer> unused) {
      coll.add(1);
    }

    @SideEffectsOnly("#1")
    void mutatesArgument(Collection<Integer> c) {
      c.add(1);
    }

    @SideEffectsOnly({"#1", "#1.noSuchMethod()"})
    // :: error: (flowexpr.parse.error.sideeffectsonly)
    void unparseable(Collection<Integer> c) {}
  }

  interface CellMutator {
    @SideEffectsOnly("#2")
    void apply(Cell cell, Collection<Integer> c);
  }

  void unboundShiftOk() {
    // `#1` of `mutatesArgument` is `#2` of `CellMutator.apply`, which the interface method's
    // annotation lists.
    CellMutator m = Cell::mutatesArgument;
  }

  void unboundShiftNotOk() {
    // `this.coll` of `mutatesOwnField` is `#1.coll` of `CellMutator.apply`, which
    // `@SideEffectsOnly("#2")` does not cover.
    // :: error: (purity.methodref)
    CellMutator m = Cell::mutatesOwnField;
  }

  interface UnparseableCellMutator {
    @SideEffectsOnly({"#1", "#1.noSuchMethod()"})
    // :: error: (flowexpr.parse.error.sideeffectsonly)
    void apply(Cell cell, Collection<Integer> c);
  }

  void unparseableNotOk() {
    // The two annotations are written identically, but `#1` of `unparseable` is `#2` of
    // `UnparseableCellMutator.apply`.  An annotation that cannot be parsed cannot be compared.
    // :: error: (purity.methodref)
    UnparseableCellMutator m = Cell::unparseable;
  }

  interface TwoMutator {
    @SideEffectsOnly({"#1", "#2"})
    void apply(StringBuilder sb1, StringBuilder sb2);
  }

  @SideEffectsOnly("#1")
  static void mutatesVarargs(StringBuilder... sbs) {}

  void varargsArityNotOk() {
    // The varargs method has one formal parameter and the interface method has two, so the two
    // `@SideEffectsOnly` annotations cannot be compared.
    // :: warning: (purity.parameters.sideeffectsonly) :: error: (purity.methodref)
    TwoMutator m = MethodRefSideEffectsOnly::mutatesVarargs;
  }

  static class VarargsCell {
    @SideEffectsOnly("this")
    void mutatesReceiver(Object... unused) {}
  }

  interface VarargsCellMutator {
    @SideEffectsOnly("#1")
    void apply(VarargsCell cell, Object a, Object b);
  }

  void varargsArityReceiverOk() {
    // The arities differ, but the receiver of an unbound reference is always the interface
    // method's `#1`, whatever the arities are.
    VarargsCellMutator m = VarargsCell::mutatesReceiver;
  }

  static class VarargsBox {
    @SideEffectsOnly("#1")
    VarargsBox(StringBuilder... sbs) {}
  }

  interface TwoMaker {
    @SideEffectsOnly({"#1", "#2"})
    VarargsBox make(StringBuilder sb1, StringBuilder sb2);
  }

  void constructorVarargsArityNotOk() {
    // :: warning: (purity.parameters.sideeffectsonly) :: error: (purity.methodref)
    TwoMaker m = VarargsBox::new;
  }

  static StringBuilder staticSb = new StringBuilder();

  interface StaticMutator {
    @SideEffectsOnly("MethodRefSideEffectsOnly.staticSb")
    void apply(StringBuilder sb1, StringBuilder sb2);
  }

  @SideEffectsOnly("MethodRefSideEffectsOnly.staticSb")
  static void mutatesStaticVarargs(StringBuilder... sbs) {}

  void varargsArityStaticOk() {
    // The annotation mentions no parameter, so the parameters need not correspond.
    StaticMutator m = MethodRefSideEffectsOnly::mutatesStaticVarargs;
  }

  static class Box {
    @SideEffectsOnly("this")
    Box(Collection<Integer> c) {}
  }

  static class Wrapper {
    Collection<Integer> c;

    @SideEffectsOnly("this.c")
    Wrapper(Collection<Integer> c) {
      this.c = c;
      this.c.add(1);
    }
  }

  interface Maker<T> {
    @SideEffectsOnly("MethodRefSideEffectsOnly.staticSb")
    T make(Collection<Integer> c);
  }

  void constructorOk() {
    // The object under construction did not exist before the call, so modifying it is not a side
    // effect that is visible to the caller.
    Maker<Box> m = Box::new;
  }

  void constructorNotOk() {
    // `this.c` is reached through the object under construction, but it is the caller's argument.
    // :: error: (purity.methodref)
    Maker<Wrapper> m = Wrapper::new;
  }

  static class Outer {
    Collection<Integer> log = new java.util.ArrayList<>();

    class Inner extends Outer {
      @SideEffectsOnly("Outer.this.log")
      void mutatesOuterLog() {
        Outer.this.log.add(1);
      }
    }
  }

  interface InnerMutator {
    @SideEffectsOnly("#1.log")
    void apply(Outer.Inner inner);
  }

  void outerThisNotOk() {
    // `Outer.this` is the enclosing instance, not the receiver, so it is not `#1`.
    // :: error: (purity.methodref)
    InnerMutator m = Outer.Inner::mutatesOuterLog;
  }

  void boundNotOk() {
    Cell cell = new Cell();
    // For a bound reference, the receiver is fixed by the reference itself, so no expression at
    // the interface method's declaration denotes what `mutatesOwnField` modifies.
    // :: error: (purity.methodref)
    Mutator m = cell::mutatesOwnField;
  }

  static class Node {
    Collection<Integer> child = new java.util.ArrayList<>();
  }

  @SideEffectsOnly("#1[0].child")
  static void mutatesFirstChild(Node... nodes) {
    nodes[0].child.add(1);
  }

  @SideEffectsOnly("#1[1].child")
  static void mutatesSecondChild(Node... nodes) {
    nodes[1].child.add(1);
  }

  interface ChildMutator {
    @SideEffectsOnly("#1.child")
    void apply(Node node);
  }

  interface TwoChildMutator {
    @SideEffectsOnly("#2.child")
    void apply(Node node1, Node node2);
  }

  void varargsElementOk() {
    // Under variable-arity invocation, `#1[0]` of the referenced method is the interface method's
    // `#1`, even though the two methods have the same number of parameters.
    ChildMutator m1 = MethodRefSideEffectsOnly::mutatesFirstChild;
    TwoChildMutator m2 = MethodRefSideEffectsOnly::mutatesSecondChild;
  }

  void varargsElementNotOk() {
    // `#1[0].child` is the interface method's `#1.child`, which its annotation does not permit
    // modifying.
    // :: error: (purity.methodref)
    TwoChildMutator m = MethodRefSideEffectsOnly::mutatesFirstChild;
  }

  static Node shared = new Node();

  interface NodeMutator {
    @SideEffectsOnly("#1")
    void apply(Node node);
  }

  @SideEffectsOnly("#1[0]")
  static void reassignsFirstThenMutates(Node... nodes) {
    nodes[0] = shared;
    nodes[0].child.add(1);
  }

  @SideEffectsOnly("#1[0]")
  static void readsFirstThenMutates(Node... nodes) {
    if (nodes.length > 0) {
      for (Node n : nodes) {}
      nodes[0].child.add(1);
    }
  }

  void varargsElementReassignedNotOk() {
    // The body stores `shared` into `#1[0]` before modifying `#1[0].child`, so what it modifies is
    // not reached through the interface method's `#1`.
    // :: warning: (purity.parameters.sideeffectsonly) :: error: (purity.methodref)
    NodeMutator m = MethodRefSideEffectsOnly::reassignsFirstThenMutates;
  }

  void varargsElementReadOk() {
    // The body only reads the elements of `nodes`, so `#1[0]` is the interface method's `#1`.
    NodeMutator m = MethodRefSideEffectsOnly::readsFirstThenMutates;
  }

  @SideEffectsOnly("#1[2147483647].child")
  static void mutatesHugeIndex(Node... nodes) {}

  void varargsElementHugeIndexNotOk() {
    // The index is beyond the interface method's parameters.
    // :: warning: (purity.parameters.sideeffectsonly) :: error: (purity.methodref)
    ChildMutator m = MethodRefSideEffectsOnly::mutatesHugeIndex;
  }

  static class Parent {
    Collection<Integer> data = new java.util.ArrayList<>();
  }

  static class Child extends Parent {
    // Hides Parent.data.
    Collection<Integer> data = new java.util.ArrayList<>();

    @SideEffectsOnly("super.data")
    void mutatesParentData() {
      super.data.add(1);
    }
  }

  interface ChildDataMutator {
    @SideEffectsOnly("#1")
    void apply(Child c);
  }

  interface NoArgMutator {
    @SideEffectsOnly("MethodRefSideEffectsOnly.staticSb")
    void apply();
  }

  void superUnboundOk() {
    // `super.data` is reached through the receiver, which is `#1` of `ChildDataMutator.apply`.
    ChildDataMutator m = Child::mutatesParentData;
  }

  void superBoundNotOk() {
    Child c = new Child();
    // :: error: (purity.methodref)
    NoArgMutator m = c::mutatesParentData;
  }

  interface LimitedFunction {
    @SideEffectsOnly("#1")
    Integer apply(Node n);
  }

  @SideEffectFree
  static LimitedFunction wrap(java.util.function.Function<Node, Integer> f) {
    // Every caller of `wrap` passes a side-effect-free `f`.
    return f::apply;
  }

  static LimitedFunction wrapUnannotated(java.util.function.Function<Node, Integer> f) {
    // :: error: (purity.methodref)
    return f::apply;
  }
}
