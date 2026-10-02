package org.checkerframework.checker.modifiability.iterator;

import java.util.Collection;
import org.checkerframework.checker.modifiability.ModifiabilityBaseChecker;
import org.checkerframework.framework.qual.RelevantJavaTypes;
import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.source.SuppressWarningsPrefix;

/**
 * A type-checker that checks whether a collection's iterator preserves the collection's
 * modifiability.
 *
 * <p>The checker enforces the iterator portion of the Modifiability type system, where
 * {@code @IteratorPolyMod} collections have iterators that preserve relevant modifiability
 * capabilities and {@code @MaybeIteratorPolyMod} collections have unknown iterator-preservation
 * behavior.
 *
 * @checker_framework.manual #modifiability-checker Modifiability Checker
 */
@RelevantJavaTypes({Collection.class})
@StubFiles({"javaparser.astub"})
@SuppressWarningsPrefix({"iterator", "modifiability"})
public class IteratorChecker extends ModifiabilityBaseChecker {
  /** Creates an IteratorChecker. */
  public IteratorChecker() {}

  @Override
  protected boolean usesIteratorChecker() {
    return false;
  }
}
