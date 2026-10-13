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

package recordpatinfer;

import java.util.function.UnaryOperator;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class JlsExample {

  // Copied from JLS Example 18.5.5-1.
  record Mapper<T>(T in, T out) implements UnaryOperator<T> {
    public T apply(T arg) {
      return in.equals(arg) ? out : null;
    }
  }

  // Copied from JLS Example 18.5.5-1.
  static class IllustrateRecordPatternTypeInference {
    void test(UnaryOperator<? extends CharSequence> op) {
      if (op instanceof Mapper(var in, var out)) {
        boolean shorter = out.length() < in.length();
      }
    }
  }

  /**
   * The JLS walks through this exact case: T is {@code UnaryOperator<? extends CharSequence>}, so
   * the inferred type of the pattern is {@code Mapper<? extends CharSequence>} and both pattern
   * variables have type {@code CharSequence}.
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
      // :: error: (assignment)
      @Untainted CharSequence a = in;
      // :: error: (assignment)
      @Untainted CharSequence b = out;
    }
  }

  /**
   * No wildcard: alpha is inferred exactly, by the constraint <UnaryOperator<T'> =
   * UnaryOperator<alpha>>.
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
      // :: error: (assignment)
      @Untainted CharSequence a = in;
    }
  }

  /**
   * Control test: the type arguments are written explicitly.
   *
   * <p>NOTE: javac (checked through JDK 25) rejects type annotations anywhere inside a record
   * pattern's type -- {@code Mapper<@Untainted CharSequence>(var in, var out)} is "error:
   * annotations not allowed on record patterns". So a programmer can never write the qualifiers of
   * a record pattern's type arguments; they must always be inferred. That makes this test a control
   * only for the erased shape of the pattern, and it makes 18.5.5 inference the only way for a
   * pattern variable to get a qualifier other than the default.
   */
  void explicitTypeArguments(UnaryOperator<@Untainted CharSequence> op) {
    if (op instanceof Mapper<CharSequence>(var in, var out)) {
      @Untainted CharSequence a = in;
      @Untainted CharSequence b = out;
    }
  }

  /**
   * The component patterns are type patterns rather than `var` patterns. DESIGN QUESTION: does the
   * pattern variable get the qualifier of the inferred component type (@Untainted, as asserted
   * here), or the default for the written type `CharSequence` (@Tainted, which would make these
   * assignments errors)? Compare AnnotatedJEP440.testPoint, which assumes the former.
   */
  void typePatternComponents(UnaryOperator<? extends @Untainted CharSequence> op) {
    if (op instanceof Mapper(CharSequence in, CharSequence out)) {
      @Untainted CharSequence a = in;
      @Untainted CharSequence b = out;
    }
  }
}
