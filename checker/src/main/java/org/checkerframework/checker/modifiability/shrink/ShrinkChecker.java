package org.checkerframework.checker.modifiability.shrink;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import org.checkerframework.checker.modifiability.ModifiabilityBaseChecker;
import org.checkerframework.framework.qual.RelevantJavaTypes;
import org.checkerframework.framework.source.SuppressWarningsPrefix;

/**
 * A type-checker that warns, at compile time, if a program might throw {@link
 * UnsupportedOperationException} at run time due to calling a shrink method on a collection.
 *
 * <p>The checker enforces the Modifiability type system, where {@code @Shrinkable} collections can
 * be safely mutated, {@code @Unshrinkable} collections cannot be shrunk, and
 * {@code @MaybeShrinkable} collections have unknown shrink behavior.
 *
 * @checker_framework.manual #modifiability-checker Modifiability Checker
 */
@RelevantJavaTypes({Collection.class, Iterator.class, Map.class, Map.Entry.class})
@SuppressWarningsPrefix({"shrinkable", "modifiability"})
public class ShrinkChecker extends ModifiabilityBaseChecker {
  /** Creates a Shrink checker. */
  public ShrinkChecker() {}
}
