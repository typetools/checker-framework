/*
 * @test
 * @summary Type-checking-performance regression test for issue #4412:
 * https://github.com/typetools/checker-framework/issues/4412
 * Four mutually recursive type variables, each bounded by a type parameterized with all four.
 * Comparing these types used to recompute AnnotatedTypeMirror hash codes over and over.
 *
 * -AslowTypecheckingSeconds is set low enough that a performance regression produces a
 * "slow.typechecking" warning, and -Werror turns that warning into a test failure.  Measured
 * through jtreg on an otherwise idle machine: under 4 seconds, versus roughly 15 seconds reported
 * without the fix this test guards.
 *
 * @compile/timeout=600 -Werror -processor org.checkerframework.checker.nullness.NullnessChecker -AslowTypecheckingSeconds=8 Issue4412.java
 */
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;

public interface Issue4412<
    LeftLeftType extends Issue4412.LeftLeft<LeftLeftType, LeftType, RightType, RightRightType>,
    LeftType extends Issue4412.Left<LeftLeftType, LeftType, RightType, RightRightType>,
    RightType extends Issue4412.Right<LeftLeftType, LeftType, RightType, RightRightType>,
    RightRightType extends
        Issue4412.RightRight<LeftLeftType, LeftType, RightType, RightRightType>> {

  <T> T reduce(
      @NonNull Function1<? super LeftLeftType, ? extends T> leftLeftReducer,
      @NonNull Function1<? super LeftType, ? extends T> leftReducer,
      @NonNull Function1<? super RightType, ? extends T> rightReducer,
      @NonNull Function1<? super RightRightType, ? extends T> rightRightReducer);

  void act(
      @NonNull VoidFunction1<? super LeftLeftType> leftLeftAction,
      @NonNull VoidFunction1<? super LeftType> leftAction,
      @NonNull VoidFunction1<? super RightType> rightAction,
      @NonNull VoidFunction1<? super RightRightType> rightRightAction);

  interface LeftLeft<
          LeftLeftType extends LeftLeft<LeftLeftType, LeftType, RightType, RightRightType>,
          LeftType extends Left<LeftLeftType, LeftType, RightType, RightRightType>,
          RightType extends Right<LeftLeftType, LeftType, RightType, RightRightType>,
          RightRightType extends RightRight<LeftLeftType, LeftType, RightType, RightRightType>>
      extends Issue4412<LeftLeftType, LeftType, RightType, RightRightType> {}

  // Not final to allow reification
  abstract class LeftLeftImpl<
          LeftLeftType extends LeftLeft<LeftLeftType, LeftType, RightType, RightRightType>,
          LeftType extends Left<LeftLeftType, LeftType, RightType, RightRightType>,
          RightType extends Right<LeftLeftType, LeftType, RightType, RightRightType>,
          RightRightType extends RightRight<LeftLeftType, LeftType, RightType, RightRightType>>
      implements LeftLeft<LeftLeftType, LeftType, RightType, RightRightType> {

    private final Class<LeftLeftType> selfClass;

    protected LeftLeftImpl(Class<LeftLeftType> selfClass) {
      this.selfClass = selfClass;
    }

    @Override
    public final <T> T reduce(
        @NonNull Function1<? super LeftLeftType, ? extends T> leftLeftReducer,
        @NonNull Function1<? super LeftType, ? extends T> leftReducer,
        @NonNull Function1<? super RightType, ? extends T> rightReducer,
        @NonNull Function1<? super RightRightType, ? extends T> rightRightReducer) {
      return leftLeftReducer.apply(getSelf());
    }

    @Override
    public final void act(
        @NonNull VoidFunction1<? super LeftLeftType> leftLeftAction,
        @NonNull VoidFunction1<? super LeftType> leftAction,
        @NonNull VoidFunction1<? super RightType> rightAction,
        @NonNull VoidFunction1<? super RightRightType> rightRightAction) {
      leftLeftAction.apply(getSelf());
    }

    private LeftLeftType getSelf() {
      return Objects.requireNonNull(selfClass.cast(this));
    }
  }

  interface Left<
          LeftLeftType extends LeftLeft<LeftLeftType, LeftType, RightType, RightRightType>,
          LeftType extends Left<LeftLeftType, LeftType, RightType, RightRightType>,
          RightType extends Right<LeftLeftType, LeftType, RightType, RightRightType>,
          RightRightType extends RightRight<LeftLeftType, LeftType, RightType, RightRightType>>
      extends Issue4412<LeftLeftType, LeftType, RightType, RightRightType> {}

  // Not final to allow reification
  abstract class LeftImpl<
          LeftLeftType extends LeftLeft<LeftLeftType, LeftType, RightType, RightRightType>,
          LeftType extends Left<LeftLeftType, LeftType, RightType, RightRightType>,
          RightType extends Right<LeftLeftType, LeftType, RightType, RightRightType>,
          RightRightType extends RightRight<LeftLeftType, LeftType, RightType, RightRightType>>
      implements Left<LeftLeftType, LeftType, RightType, RightRightType> {

    private final Class<LeftType> selfClass;

    protected LeftImpl(@NonNull Class<LeftType> selfClass) {
      this.selfClass = selfClass;
    }

    @Override
    public final <T> T reduce(
        @NonNull Function1<? super LeftLeftType, ? extends T> leftLeftReducer,
        @NonNull Function1<? super LeftType, ? extends T> leftReducer,
        @NonNull Function1<? super RightType, ? extends T> rightReducer,
        @NonNull Function1<? super RightRightType, ? extends T> rightRightReducer) {
      return leftReducer.apply(getSelf());
    }

    @Override
    public final void act(
        @NonNull VoidFunction1<? super LeftLeftType> leftLeftAction,
        @NonNull VoidFunction1<? super LeftType> leftAction,
        @NonNull VoidFunction1<? super RightType> rightAction,
        @NonNull VoidFunction1<? super RightRightType> rightRightAction) {
      leftAction.apply(getSelf());
    }

    private LeftType getSelf() {
      return Objects.requireNonNull(selfClass.cast(this));
    }
  }

  interface Right<
          LeftLeftType extends LeftLeft<LeftLeftType, LeftType, RightType, RightRightType>,
          LeftType extends Left<LeftLeftType, LeftType, RightType, RightRightType>,
          RightType extends Right<LeftLeftType, LeftType, RightType, RightRightType>,
          RightRightType extends RightRight<LeftLeftType, LeftType, RightType, RightRightType>>
      extends Issue4412<LeftLeftType, LeftType, RightType, RightRightType> {}

  // Not final to allow reification
  abstract class RightImpl<
          LeftLeftType extends LeftLeft<LeftLeftType, LeftType, RightType, RightRightType>,
          LeftType extends Left<LeftLeftType, LeftType, RightType, RightRightType>,
          RightType extends Right<LeftLeftType, LeftType, RightType, RightRightType>,
          RightRightType extends RightRight<LeftLeftType, LeftType, RightType, RightRightType>>
      implements Right<LeftLeftType, LeftType, RightType, RightRightType> {

    private final Class<RightType> selfClass;

    protected RightImpl(@NonNull Class<RightType> selfClass) {
      this.selfClass = selfClass;
    }

    @Override
    public final <T> T reduce(
        @NonNull Function1<? super LeftLeftType, ? extends T> leftLeftReducer,
        @NonNull Function1<? super LeftType, ? extends T> leftReducer,
        @NonNull Function1<? super RightType, ? extends T> rightReducer,
        @NonNull Function1<? super RightRightType, ? extends T> rightRightReducer) {
      return rightReducer.apply(getSelf());
    }

    @Override
    public final void act(
        @NonNull VoidFunction1<? super LeftLeftType> leftLeftAction,
        @NonNull VoidFunction1<? super LeftType> leftAction,
        @NonNull VoidFunction1<? super RightType> rightAction,
        @NonNull VoidFunction1<? super RightRightType> rightRightAction) {
      rightAction.apply(getSelf());
    }

    private RightType getSelf() {
      return Objects.requireNonNull(selfClass.cast(this));
    }
  }

  interface RightRight<
          LeftLeftType extends LeftLeft<LeftLeftType, LeftType, RightType, RightRightType>,
          LeftType extends Left<LeftLeftType, LeftType, RightType, RightRightType>,
          RightType extends Right<LeftLeftType, LeftType, RightType, RightRightType>,
          RightRightType extends RightRight<LeftLeftType, LeftType, RightType, RightRightType>>
      extends Issue4412<LeftLeftType, LeftType, RightType, RightRightType> {}

  // Not final to allow reification
  abstract class RightRightImpl<
          LeftLeftType extends LeftLeft<LeftLeftType, LeftType, RightType, RightRightType>,
          LeftType extends Left<LeftLeftType, LeftType, RightType, RightRightType>,
          RightType extends Right<LeftLeftType, LeftType, RightType, RightRightType>,
          RightRightType extends RightRight<LeftLeftType, LeftType, RightType, RightRightType>>
      implements RightRight<LeftLeftType, LeftType, RightType, RightRightType> {

    private final Class<RightRightType> selfClass;

    protected RightRightImpl(@NonNull Class<RightRightType> selfClass) {
      this.selfClass = selfClass;
    }

    @Override
    public final <T> T reduce(
        @NonNull Function1<? super LeftLeftType, ? extends T> leftLeftReducer,
        @NonNull Function1<? super LeftType, ? extends T> leftReducer,
        @NonNull Function1<? super RightType, ? extends T> rightReducer,
        @NonNull Function1<? super RightRightType, ? extends T> rightRightReducer) {
      return rightRightReducer.apply(getSelf());
    }

    @Override
    public final void act(
        @NonNull VoidFunction1<? super LeftLeftType> leftLeftAction,
        @NonNull VoidFunction1<? super LeftType> leftAction,
        @NonNull VoidFunction1<? super RightType> rightAction,
        @NonNull VoidFunction1<? super RightRightType> rightRightAction) {
      rightRightAction.apply(getSelf());
    }

    private RightRightType getSelf() {
      return Objects.requireNonNull(selfClass.cast(this));
    }
  }

  interface VoidFunction1<T> {

    void apply(@NonNull T t);
  }

  interface Function1<T, R> {

    @NonNull R apply(@NonNull T t);
  }
}
