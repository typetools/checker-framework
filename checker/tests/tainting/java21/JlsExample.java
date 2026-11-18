// @below-java21-jdk-skip-test

// None of the WPI formats support the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// The `Mapper` record and the `IllustrateRecordPatternTypeInference` class are copied from
// Example 18.5.5-1 ("Record Pattern Type Inference") of the Java Language Specification,
// Java SE 26 Edition (JLS section 18.5.5).  The annotations and the additional test methods
// are not part of the JLS example.

import java.util.function.UnaryOperator;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class JlsExample {

  record Mapper<T>(T in, T out) implements UnaryOperator<T> {
    public T apply(T arg) {
      return in.equals(arg) ? out : null;
    }
  }

  static class IllustrateRecordPatternTypeInference {
    void test(UnaryOperator<? extends CharSequence> op) {
      if (op instanceof Mapper(var in, var out)) {
        boolean shorter = out.length() < in.length();
      }
    }
  }

  /**
   * The JLS walks through this exact case: the matched type is {@code UnaryOperator<? extends
   * CharSequence>}, so the inferred type of the pattern is {@code Mapper<? extends CharSequence>}
   * and both pattern variables have type {@code CharSequence}.
   *
   * <p>Annotated, the wildcard's upper bound is {@code @Untainted CharSequence}, so both pattern
   * variables should have type {@code @Untainted CharSequence}.
   */
  void jlsExampleUntainted(UnaryOperator<? extends @Untainted CharSequence> op) {
    if (op instanceof Mapper(var in, var out)) {
      @Untainted CharSequence a = in;
      @Untainted CharSequence b = out;
    }
  }

  /** Same, but the upper bound is {@code @Tainted}, so both assignments should be errors. */
  void jlsExampleTainted(UnaryOperator<? extends @Tainted CharSequence> op) {
    if (op instanceof Mapper(var in, var out)) {
      // :: error: [assignment]
      @Untainted CharSequence a = in;
      // :: error: [assignment]
      @Untainted CharSequence b = out;
    }
  }

  /**
   * No wildcard: alpha, the inference variable for Mapper's type parameter, is inferred exactly, by
   * the constraint <UnaryOperator<@Untainted CharSequence> = UnaryOperator<alpha>>.
   */
  void invariantUntainted(UnaryOperator<@Untainted CharSequence> op) {
    if (op instanceof Mapper(var in, var out)) {
      @Untainted CharSequence a = in;
      @Untainted CharSequence b = out;
    }
  }

  /** No wildcard, tainted. */
  void invariantTainted(UnaryOperator<@Tainted CharSequence> op) {
    if (op instanceof Mapper(var in, var out)) {
      // :: error: [assignment]
      @Untainted CharSequence a = in;
    }
  }

  /**
   * Control test: the type arguments are written explicitly.
   *
   * <p>NOTE: JLS 14.30.1 forbids type annotations anywhere in a record pattern's type: javac
   * rejects {@code Mapper<@Untainted CharSequence>(var in, var out)} with "error: annotations not
   * allowed on record patterns". So a programmer can never write the qualifiers of a record
   * pattern's type arguments, and this test is a control only for the unannotated Java type of the
   * pattern; the qualifiers of the pattern's type arguments always come from 18.5.5 inference.
   */
  void explicitTypeArguments(UnaryOperator<@Untainted CharSequence> op) {
    if (op instanceof Mapper<CharSequence>(var in, var out)) {
      @Untainted CharSequence a = in;
      @Untainted CharSequence b = out;
    }
  }

  /**
   * The nested patterns are type patterns rather than `var` patterns. Dataflow refines each pattern
   * variable to the qualifier of the inferred type of its record component, rather than leaving the
   * default for the written type `CharSequence`.
   */
  void typePatternComponents(UnaryOperator<? extends @Untainted CharSequence> op) {
    if (op instanceof Mapper(CharSequence in, CharSequence out)) {
      @Untainted CharSequence a = in;
      @Untainted CharSequence b = out;
    }
  }
}
