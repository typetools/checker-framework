package org.checkerframework.checker.modifiability.replace;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import org.checkerframework.checker.modifiability.ModifiabilityBaseChecker;
import org.checkerframework.framework.qual.RelevantJavaTypes;
import org.checkerframework.framework.source.SuppressWarningsPrefix;

/**
 * A type-checker that warns, at compile time, if a program might throw {@link
 * UnsupportedOperationException} at run time due to calling a replace method on a collection.
 *
 * <p>The checker enforces the Modifiability type system, where {@code @Replaceable} collections can
 * be safely mutated, {@code @Unreplaceable} collections cannot be replaced, and
 * {@code @MaybeReplaceable} collections have unknown replace behavior.
 *
 * @checker_framework.manual #modifiability-checker Modifiability Checker
 */
@RelevantJavaTypes({Collection.class, Iterator.class, Map.class, Map.Entry.class})
@SuppressWarningsPrefix({"replaceable", "modifiability"})
public class ReplaceChecker extends ModifiabilityBaseChecker {
  /** Creates a Replace checker. */
  public ReplaceChecker() {}
}
