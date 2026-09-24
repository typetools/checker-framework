package org.checkerframework.checker.modifiability.grow;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import org.checkerframework.checker.modifiability.ModifiabilityBaseChecker;
import org.checkerframework.framework.qual.RelevantJavaTypes;
import org.checkerframework.framework.source.SuppressWarningsPrefix;

/**
 * A type-checker that warns, at compile time, if a program might throw {@link
 * UnsupportedOperationException} at run time due to calling a grow method on a collection.
 *
 * <p>The checker enforces the Modifiability type system, where {@code @Growable} collections can be
 * safely added to, {@code @Ungrowable} collections cannot, and {@code @MaybeGrowable} collections
 * have unknown grow behavior.
 *
 * @checker_framework.manual #modifiability-checker Modifiability Checker
 */
@RelevantJavaTypes({Collection.class, Iterator.class, Map.class, Map.Entry.class})
@SuppressWarningsPrefix({"growable", "modifiability"})
public class GrowChecker extends ModifiabilityBaseChecker {
  /** Creates a GrowChecker. */
  public GrowChecker() {}
}
