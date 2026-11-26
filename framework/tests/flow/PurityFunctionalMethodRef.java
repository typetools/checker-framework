// A method reference is checked like an override: a call through the functional method must
// guarantee, of the arguments to the referenced method's functional-interface parameters, the
// purity that the referenced method's body assumes of them.

import java.util.function.Function;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class PurityFunctionalMethodRef {

  @SideEffectFree
  static int applyStatic(Function<String, Integer> f, String s) {
    return f.apply(s);
  }

  @SideEffectFree
  int applyInstance(Function<String, Integer> f, String s) {
    return f.apply(s);
  }

  @FunctionalInterface
  interface Applier {
    @SideEffectFree
    int apply(Function<String, Integer> f, String s);
  }

  @FunctionalInterface
  interface UnboundApplier {
    @SideEffectFree
    int apply(PurityFunctionalMethodRef receiver, Function<String, Integer> f, String s);
  }

  @FunctionalInterface
  interface GenericApplier<T> {
    @SideEffectFree
    int apply(T f, String s);
  }

  @FunctionalInterface
  interface UnannotatedApplier {
    int apply(Function<String, Integer> f, String s);
  }

  void references() {
    Applier a = PurityFunctionalMethodRef::applyStatic;
    Applier b = this::applyInstance;
    UnboundApplier c = PurityFunctionalMethodRef::applyInstance;
    // :: error: [purity.methodref.functional.parameter]
    GenericApplier<Function<String, Integer>> d = PurityFunctionalMethodRef::applyStatic;
    // A call through UnannotatedApplier checks nothing, so it can run applyStatic with a function
    // that has side effects.
    // :: error: [purity.methodref.functional.parameter]
    UnannotatedApplier e = PurityFunctionalMethodRef::applyStatic;
  }

  /** The body relies on the purity of calls to u.apply, but those calls check no argument. */
  @SideEffectFree
  static int runsUnannotated(UnannotatedApplier u) {
    return u.apply(s -> 0, "");
  }

  /** The body relies on the purity of calls to a.apply, and those calls check their argument. */
  @SideEffectFree
  static int runsAnnotated(Applier a) {
    return a.apply(s -> 0, "");
  }

  void passesReferences() {
    // :: error: [purity.methodref.functional.parameter]
    runsUnannotated(PurityFunctionalMethodRef::applyStatic);
    runsAnnotated(PurityFunctionalMethodRef::applyStatic);
  }

  void genericReferences() {
    // Every argument of a call through GenericApplier<PureFunction> is a PureFunction, whose apply
    // method is side-effect-free.
    GenericApplier<PureFunction> f = PurityFunctionalMethodRef::applyStatic;
    // :: error: [purity.methodref.functional.parameter]
    GenericApplier<Function<String, Integer>> g = PurityFunctionalMethodRef::applyStatic;
  }

  /** A subinterface that redeclares {@code Function.apply}, with a stronger guarantee. */
  @FunctionalInterface
  interface PureFunction extends Function<String, Integer> {
    @Override
    @SideEffectFree
    Integer apply(String s);
  }

  @FunctionalInterface
  interface SubApplier {
    @SideEffectFree
    int apply(PureFunction f, String s);
  }

  /** Redeclares {@code UnannotatedApplier.apply}, which a call can still go through. */
  @FunctionalInterface
  interface StrongApplier extends UnannotatedApplier {
    @Override
    @SideEffectFree
    int apply(Function<String, Integer> f, String s);
  }

  void overriddenFunctionalMethod() {
    // A call through UnannotatedApplier.apply can run the reference.
    // :: error: [purity.methodref.functional.parameter]
    StrongApplier a = PurityFunctionalMethodRef::applyStatic;
  }

  /**
   * Inherits two functional methods, {@code Applier.apply} and {@code UnannotatedApplier.apply},
   * neither of which overrides the other.
   */
  @FunctionalInterface
  interface StrongFirstApplier extends Applier, UnannotatedApplier {}

  /** Like {@link StrongFirstApplier}, but with its superinterfaces in the other order. */
  @FunctionalInterface
  interface UnannotatedFirstApplier extends UnannotatedApplier, Applier {}

  void inheritedFunctionalMethods() {
    // A call through UnannotatedApplier.apply can run the reference.
    // :: error: [purity.methodref.functional.parameter]
    StrongFirstApplier a = PurityFunctionalMethodRef::applyStatic;
    // :: error: [purity.methodref.functional.parameter]
    UnannotatedFirstApplier b = PurityFunctionalMethodRef::applyStatic;
  }

  @SideEffectFree
  static int ignoreStatic(Function<String, Integer> f, String s) {
    return 0;
  }

  @SideEffectFree
  static int storeStatic(Function<String, Integer> f, String s) {
    Function<String, Integer> g = f;
    return f == null ? 0 : f.hashCode();
  }

  @SideEffectFree
  final int ignoreFinal(Function<String, Integer> f, String s) {
    return 0;
  }

  @SideEffectFree
  int ignoreOverridable(Function<String, Integer> f, String s) {
    return 0;
  }

  @SideEffectFree
  static int passStatic(Function<String, Integer> f, String s) {
    return applyStatic(f, s);
  }

  @SideEffectFree
  static int overwriteStatic(Function<String, Integer> f, String s) {
    f = null;
    return 0;
  }

  @SideEffectFree
  static int overwriteArrayStatic(Function<String, Integer> f, String s) {
    int[] a = new int[1];
    // :: error: [purity.assign.array]
    a[f.apply(s)] = 0;
    return 0;
  }

  void bodyDoesNotRelyOnParameter() {
    // None of these bodies relies on the purity of f, so no call can make one run impure code.
    UnannotatedApplier a = PurityFunctionalMethodRef::ignoreStatic;
    UnannotatedApplier b = PurityFunctionalMethodRef::storeStatic;
    UnannotatedApplier c = this::ignoreFinal;
    UnannotatedApplier f = PurityFunctionalMethodRef::overwriteStatic;
    // The index of the array access calls f.
    // :: error: [purity.methodref.functional.parameter]
    UnannotatedApplier g = PurityFunctionalMethodRef::overwriteArrayStatic;
    // An override of ignoreOverridable might call f.
    // :: error: [purity.methodref.functional.parameter]
    UnannotatedApplier d = this::ignoreOverridable;
    // passStatic relies on the purity of f to call applyStatic.
    // :: error: [purity.methodref.functional.parameter]
    UnannotatedApplier e = PurityFunctionalMethodRef::passStatic;
  }

  static class SuperReferences extends PurityFunctionalMethodRef {
    @Override
    @SideEffectFree
    int ignoreOverridable(Function<String, Integer> f, String s) {
      return applyStatic(f, s);
    }

    void superReferences() {
      // A `super` method reference runs PurityFunctionalMethodRef.ignoreOverridable itself, not
      // an override of it, and that body does not rely on the purity of f.
      UnannotatedApplier a = super::ignoreOverridable;
      // This reference runs the override, which relies on the purity of f.
      // :: error: [purity.methodref.functional.parameter]
      UnannotatedApplier b = this::ignoreOverridable;
    }
  }

  /** A subinterface that redeclares {@code Function.apply}, with no purity annotation. */
  @FunctionalInterface
  interface PlainFunction extends Function<String, Integer> {
    @Override
    Integer apply(String s);
  }

  @FunctionalInterface
  interface PlainSubApplier {
    @SideEffectFree
    int apply(PlainFunction f, String s);
  }

  void contravariantCallbackParameter() {
    // A call through SubApplier passes a PureFunction, whose apply is what applyStatic's call to
    // Function.apply runs.
    SubApplier a = PurityFunctionalMethodRef::applyStatic;
    // PlainFunction.apply promises nothing, but a call through PlainSubApplier.apply checks that
    // its argument's implementation of PlainFunction.apply, which is also its implementation of
    // Function.apply, is side-effect-free.
    PlainSubApplier b = PurityFunctionalMethodRef::applyStatic;
  }

  /** A class, not a functional interface, that implements {@code Function.apply} purely. */
  static final class PureImpl implements Function<String, Integer> {
    @Override
    @SideEffectFree
    public Integer apply(String s) {
      return 0;
    }
  }

  /** A class, not a functional interface, that implements {@code Function.apply} impurely. */
  static final class ImpureImpl implements Function<String, Integer> {
    @Override
    public Integer apply(String s) {
      return 0;
    }
  }

  @FunctionalInterface
  interface PureImplApplier {
    int apply(PureImpl f, String s);
  }

  @FunctionalInterface
  interface ImpureImplApplier {
    int apply(ImpureImpl f, String s);
  }

  void classCallbackParameter() {
    // Every argument is a PureImpl, whose apply is side-effect-free.
    PureImplApplier a = PurityFunctionalMethodRef::applyStatic;
    // :: error: [purity.methodref.functional.parameter]
    ImpureImplApplier b = PurityFunctionalMethodRef::applyStatic;
    // A call through GenericApplier<PureImpl>.apply checks nothing about its argument, but every
    // argument is a PureImpl.
    GenericApplier<PureImpl> c = PurityFunctionalMethodRef::applyStatic;
    // :: error: [purity.methodref.functional.parameter]
    GenericApplier<ImpureImpl> d = PurityFunctionalMethodRef::applyStatic;
  }

  @FunctionalInterface
  interface StringGetter {
    String get();
  }

  @FunctionalInterface
  interface ObjectGetter {
    Object get();
  }

  /**
   * Inherits {@code StringGetter.get} and {@code ObjectGetter.get}; neither overrides the other.
   */
  @FunctionalInterface
  interface BothGetter extends StringGetter, ObjectGetter {}

  @SideEffectFree
  static boolean callObjectGetter(ObjectGetter g) {
    return g.get() == null;
  }

  @FunctionalInterface
  interface BothGetterCaller {
    @SideEffectFree
    boolean call(BothGetter g);
  }

  void siblingCallbackMethods() {
    // A call through BothGetterCaller.call checks the argument's implementation of BothGetter's
    // functional method, which is also its implementation of ObjectGetter.get.
    BothGetterCaller a = PurityFunctionalMethodRef::callObjectGetter;
    // A call through GenericCaller<BothGetter>.call checks the argument's implementation of
    // StringGetter.get, which in a BothGetter is also its implementation of ObjectGetter.get.
    GenericCaller<BothGetter> b = PurityFunctionalMethodRef::callObjectGetter;
    // Likewise for an argument whose type is an intersection of the two getters.
    IntersectionCaller c = PurityFunctionalMethodRef::callObjectGetter;
    // A call through OverloadCaller.call checks nothing, because its parameter's type has two
    // different `get` methods and so is not a functional interface.
    // :: error: [purity.methodref.functional.parameter]
    OverloadCaller d = PurityFunctionalMethodRef::callObjectGetter;
  }

  @FunctionalInterface
  interface GenericCaller<T extends StringGetter> {
    @SideEffectFree
    boolean call(T g);
  }

  @FunctionalInterface
  interface IntersectionCaller {
    @SideEffectFree
    <T extends ObjectGetter & StringGetter> boolean call(T g);
  }

  /** Declares a {@code get} method that overloads {@code ObjectGetter.get}. */
  @FunctionalInterface
  interface ArgGetter {
    Object get(String s);
  }

  @FunctionalInterface
  interface OverloadCaller {
    @SideEffectFree
    <T extends ObjectGetter & ArgGetter> boolean call(T g);
  }

  @FunctionalInterface
  interface UnannotatedGenericApplier<F> {
    int apply(F f, String s);
  }

  void intersectionCast() {
    Object a = (Applier & java.io.Serializable) PurityFunctionalMethodRef::applyStatic;
    // A call through UnannotatedApplier.apply can run the reference.
    // :: error: [purity.methodref.functional.parameter]
    Object b = (Applier & UnannotatedApplier) PurityFunctionalMethodRef::applyStatic;
    // :: error: [purity.methodref.functional.parameter]
    Object c = (UnannotatedApplier & Applier) PurityFunctionalMethodRef::applyStatic;
    // A call through UnannotatedGenericApplier.apply can run the reference.  The methodref.param
    // error is because the FlowTestChecker's lower bound of the wildcard is @Bottom.
    Object d =
        (UnannotatedGenericApplier<? super Function<String, Integer>> & java.io.Serializable)
            // :: error: [methodref.param] :: error: [purity.methodref.functional.parameter]
            PurityFunctionalMethodRef::applyStatic;
  }

  /**
   * Implements {@code Function.apply} with a side-effect-free default method, and declares an
   * unrelated functional method.
   */
  @FunctionalInterface
  interface PureDefaultFunction extends Function<String, Integer> {
    @Override
    @SideEffectFree
    default Integer apply(String s) {
      return 0;
    }

    int other();
  }

  /** Implements {@code Function.apply} with a default method that has no purity annotation. */
  @FunctionalInterface
  interface ImpureDefaultFunction extends Function<String, Integer> {
    @Override
    default Integer apply(String s) {
      return 0;
    }

    int other();
  }

  @FunctionalInterface
  interface PureDefaultApplier {
    @SideEffectFree
    int apply(PureDefaultFunction f, String s);
  }

  @FunctionalInterface
  interface ImpureDefaultApplier {
    @SideEffectFree
    int apply(ImpureDefaultFunction f, String s);
  }

  void defaultImplementationOfCallback() {
    // applyStatic calls Function.apply, which a PureDefaultFunction implements with a
    // side-effect-free method; a call through PureDefaultApplier checks only `other`.
    PureDefaultApplier a = PurityFunctionalMethodRef::applyStatic;
    // :: error: [purity.methodref.functional.parameter]
    ImpureDefaultApplier b = PurityFunctionalMethodRef::applyStatic;
  }

  /** A class, not a functional interface, whose implementation of apply is side-effect-free. */
  static final class PureFixedFunction implements Function<String, Integer> {
    @Override
    @SideEffectFree
    public Integer apply(String s) {
      return 0;
    }
  }

  /** A class whose implementation of apply has no purity annotation. */
  static final class ImpureFixedFunction implements Function<String, Integer> {
    @Override
    public Integer apply(String s) {
      return 0;
    }
  }

  @FunctionalInterface
  interface PureFixedApplier {
    int apply(PureFixedFunction f, String s);
  }

  @FunctionalInterface
  interface ImpureFixedApplier {
    int apply(ImpureFixedFunction f, String s);
  }

  void classTypedCallback() {
    // A call through PureFixedApplier checks nothing, but every PureFixedFunction implements
    // Function.apply with a side-effect-free method.
    PureFixedApplier a = PurityFunctionalMethodRef::applyStatic;
    // :: error: [purity.methodref.functional.parameter]
    ImpureFixedApplier b = PurityFunctionalMethodRef::applyStatic;
  }

  @FunctionalInterface
  interface GenericConsumer<T> {
    void accept(T t);
  }

  @FunctionalInterface
  interface StringConsumer {
    void accept(String s);
  }

  /**
   * As a member of {@code Split<String>}, {@code GenericConsumer.accept} has the same signature as
   * {@code StringConsumer.accept}, but {@code Split} implements them with different methods.
   */
  static class Split<X> implements GenericConsumer<X>, StringConsumer {
    @Override
    @SideEffectFree
    public void accept(X x) {}

    @Override
    public void accept(String s) {}
  }

  /** Like {@link Split}, but not generic, so it implements both methods with one method. */
  static class Joined implements GenericConsumer<String>, StringConsumer {
    @Override
    public void accept(String s) {}
  }

  @SideEffectFree
  static void callStringConsumer(StringConsumer c) {
    c.accept("");
  }

  @FunctionalInterface
  interface GenericConsumerCaller<T extends GenericConsumer<String>> {
    @SideEffectFree
    void call(T t);
  }

  void splitImplementations() {
    // A call through GenericConsumerCaller checks the argument's implementation of
    // GenericConsumer.accept, but callStringConsumer runs Split.accept(String).
    // :: error: [purity.methodref.functional.parameter]
    GenericConsumerCaller<Split<String>> a = PurityFunctionalMethodRef::callStringConsumer;
    // Every Joined implements both methods with the method that the call checks.
    GenericConsumerCaller<Joined> b = PurityFunctionalMethodRef::callStringConsumer;
  }

  @FunctionalInterface
  interface SefObjectGetter {
    @SideEffectFree
    Object get();
  }

  /**
   * Every implementation of {@code ObjectGetter.get} also implements {@code SefObjectGetter.get}.
   */
  @FunctionalInterface
  interface PlainFirstGetter extends ObjectGetter, SefObjectGetter {}

  /** Like {@link PlainFirstGetter}, but with its superinterfaces in the other order. */
  @FunctionalInterface
  interface SefFirstGetter extends SefObjectGetter, ObjectGetter {}

  @FunctionalInterface
  interface PlainFirstGetterCaller {
    boolean call(PlainFirstGetter g);
  }

  @FunctionalInterface
  interface SefFirstGetterCaller {
    boolean call(SefFirstGetter g);
  }

  void siblingPurity(PlainFirstGetter p, SefFirstGetter s) {
    // A call checks nothing about its argument, but the argument's implementation of
    // ObjectGetter.get also implements SefObjectGetter.get, so it is side-effect-free.
    PlainFirstGetterCaller a = PurityFunctionalMethodRef::callObjectGetter;
    SefFirstGetterCaller b = PurityFunctionalMethodRef::callObjectGetter;
    callObjectGetter(p);
    callObjectGetter(s);
  }

  /** Callers must pass a side-effect-free getter. */
  @SideEffectFree
  static Object boundReferenceToParameter(ObjectGetter g) {
    // A call to PlainFirstGetter.get, which can run g.get, must be side-effect-free, and g.get is.
    PlainFirstGetter a = g::get;
    PlainFirstGetter b = () -> g.get();
    return a;
  }

  @SideEffectFree
  static Object boundReferenceToReassignedParameter(ObjectGetter g) {
    g = () -> null;
    // :: error: [purity.methodref]
    PlainFirstGetter a = g::get;
    return a;
  }

  @SideEffectFree
  static Object boundReferenceToLocal(ObjectGetter g) {
    ObjectGetter local = g;
    // :: error: [purity.methodref]
    PlainFirstGetter a = local::get;
    return a;
  }

  static Object boundReferenceInImpureMethod(ObjectGetter g) {
    // :: error: [purity.methodref]
    PlainFirstGetter a = g::get;
    return a;
  }
}
