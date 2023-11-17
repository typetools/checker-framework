import java.util.Optional;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.optional.qual.Present;

/** Test JDK annotations. */
@SuppressWarnings("optional.parameter")
public class JdkCheck {

  boolean isPresentTest1(@Present Optional<String> pos) {
    return pos.isPresent();
  }

  boolean isPresentTest2(Optional<String> mos) {
    return mos.isPresent();
  }

  String orElseThrowTest1(
      @Present Optional<String> pos, Supplier<RuntimeException> exceptionSupplier) {
    return pos.orElseThrow(exceptionSupplier);
  }

  String orElseThrowTest2(Optional<String> mos, Supplier<RuntimeException> exceptionSupplier) {
    return mos.orElseThrow(exceptionSupplier);
  }

  String orElseThrowTestFlow(Optional<String> mos, Supplier<RuntimeException> exceptionSupplier) {
    mos.orElseThrow(exceptionSupplier);
    return mos.get();
  }

  String getTest1(@Present Optional<String> pos) {
    return pos.get();
  }

  String getTest2(Optional<String> mos) {
    // :: error: (method.invocation)
    return mos.get();
  }

  @Present Optional<String> ofTestPNn(String s) {
    return Optional.of(s);
  }

  Optional<String> ofTestMNn(String s) {
    return Optional.of(s);
  }

  @Present Optional<String> ofTestPNble(@Nullable String s) {
    // TODO :: error: (of.nullable.argument) :: error: (return)
    return Optional.of(s);
  }

  Optional<String> ofTestMNble(@Nullable String s) {
    // TODO :: error: (of.nullable.argument) :: error: (return)
    return Optional.of(s);
  }

  @Present Optional<String> ofNullableTestPNble(@Nullable String s) {
    // :: error: (return)
    return Optional.ofNullable(s);
  }

  /* TODO: ofNullable with non-null arg gives @Present (+ a warning?)
  @Present Optional<String> ofNullableTestPNn(String s) {
      return Optional.ofNullable(s);
  }
  */

  Optional<String> ofNullableTestMNble(@Nullable String s) {
    return Optional.ofNullable(s);
  }

  Optional<String> ofNullableTestMNn(String s) {
    return Optional.ofNullable(s);
  }

  /* TODO: these will fail until Optional#map is properly annotated in the JDK.
  String mapTestGetOnPresent(@Nullable String s1, String s2) {
    Optional<String> optS1 = Optional.ofNullable(s1); // optS1 has type @MaybePresent
    if (!optS1.isPresent()) {
      return "Empty";
    }
    // From here, optS1 has the refined type @Present.
    return optS1.map(s -> s + s2).get();
  }

  String mapTestGetOnEmpty(@Nullable String s1, String s2) {
    Optional<String> optS1 = Optional.ofNullable(s1); // optS1 has type @MaybePresent
    if (optS1.isPresent()) {
      return optS1.map(s -> s + s2).get();
    } else {
      // Still has type @MaybePresent
      // :: error: (method.invocation)
      return optS1.map(s -> s + s2).get();
    }
  }
   */
}
