import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class MemberReferenceInference {
  void clever2(
      Stream<Optional<BigDecimal>> taintedStream,
      Stream<Optional<@Untainted BigDecimal>> untaintedStream) {
    // :: error: (type.arguments.not.inferred)
    Stream<@Untainted BigDecimal> s = taintedStream.map(Optional::get);
    Stream<@Untainted BigDecimal> s2 = untaintedStream.map(Optional::get);
    Stream<@Tainted BigDecimal> s3 = taintedStream.map(Optional::get);
    Stream<@Tainted BigDecimal> s4 = untaintedStream.map(Optional::get);
  }

  interface MyClass<Q> {
    String getName();
  }

  void method(
      MyClass<? extends String> clazz,
      Map<MyClass<? extends String>, @Untainted String> annotationClassNames) {
    // :: error: (type.arguments.not.inferred)
    String canonicalName = annotationClassNames.computeIfAbsent(clazz, MyClass::getName);
  }

  void method2(
      MyClass<? extends String> clazz,
      Map<MyClass<? extends String>, String> annotationClassNames) {
    String canonicalName = annotationClassNames.computeIfAbsent(clazz, MyClass::getName);
  }
}
