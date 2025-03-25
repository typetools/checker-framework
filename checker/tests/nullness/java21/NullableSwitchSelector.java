import org.checkerframework.checker.nullness.qual.Nullable;

// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 languages features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test
public class NullableSwitchSelector {

  static String formatterPatternSwitch1(@Nullable Object obj) {
    return switch (obj) {
      case Integer i -> obj.toString();
      case String s -> String.format("String %s", s);
      // :: error: (dereference.of.nullable)
      case null -> obj.toString();
      default -> obj.toString();
    };
  }

  static String formatterPatternSwitch2(@Nullable Object obj) {
    // :: error: (switching.nullable)
    return switch (obj) {
      case Integer i -> obj.toString();
      case String s -> String.format("String %s", s);
      // TODO: If obj is null, this case isn't reachable, because a null pointer exception happens
      // at the selector expression.
      // :: error: (dereference.of.nullable)
      default -> obj.toString();
    };
  }

  static String formatterPatternSwitch3(@Nullable Object obj) {
    return switch (obj) {
      case Integer i -> obj.toString();
      case String s -> String.format("String %s", s);
      // :: error: (dereference.of.nullable)
      case null, default -> obj.toString();
    };
  }
}
