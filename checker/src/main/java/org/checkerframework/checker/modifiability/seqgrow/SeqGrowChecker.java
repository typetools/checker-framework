package org.checkerframework.checker.modifiability.seqgrow;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import org.checkerframework.checker.modifiability.ModifiabilityBaseChecker;
import org.checkerframework.framework.qual.RelevantJavaTypes;
import org.checkerframework.framework.source.SuppressWarningsPrefix;

/**
 * A type-checker that warns, at compile time, if a program might throw {@link
 * UnsupportedOperationException} at run time due to calling a sequenced-grow method on a
 * collection.
 *
 * <p>The checker enforces the Modifiability type system, where {@code @SeqGrowable} collections can
 * be safely added to at the front or back, {@code @SeqUngrowable} collections cannot, and
 * {@code @MaybeSeqGrowable} collections have unknown sequenced-grow behavior.
 *
 * @checker_framework.manual #modifiability-checker Modifiability Checker
 */
@RelevantJavaTypes({Collection.class, Iterator.class, Map.class, Map.Entry.class})
@SuppressWarningsPrefix({"seqgrowable", "modifiability"})
public class SeqGrowChecker extends ModifiabilityBaseChecker {
  /** Creates a SeqGrowChecker. */
  public SeqGrowChecker() {}

  @Override
  protected boolean usesIteratorChecker() {
    // An iterator has no sequenced-grow methods, so this checker never refines an iterator result.
    return false;
  }
}
