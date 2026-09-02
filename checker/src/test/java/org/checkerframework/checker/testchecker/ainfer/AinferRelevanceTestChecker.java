package org.checkerframework.checker.testchecker.ainfer;

import java.util.Map;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.qual.RelevantJavaTypes;

/**
 * Like {@link AinferTestChecker}, but only some Java types are relevant. It uses the same type
 * system and the same visitor.
 *
 * <p>This checker exists so that the tests exercise whole-program inference for a checker that
 * restricts the Java types on which its qualifiers may be written. {@code int} is relevant because
 * it is listed. {@code String} and {@code CharSequence} are relevant because of subtyping. Arrays
 * are irrelevant, because {@code Object[].class} is not listed.
 *
 * <p>{@code Map.Entry} is listed because it is a nested type and it has type parameters, so a
 * program may refer to {@code Map.Entry} in several different ways.
 */
@RelevantJavaTypes({CharSequence.class, int.class, Map.Entry.class})
public class AinferRelevanceTestChecker extends BaseTypeChecker {

  /** Creates an AinferRelevanceTestChecker. */
  public AinferRelevanceTestChecker() {}

  @Override
  protected BaseTypeVisitor<?> createSourceVisitor() {
    return new AinferTestVisitor(this);
  }
}
