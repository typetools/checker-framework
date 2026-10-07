package org.checkerframework.checker.modifiability;

import java.util.Collection;
import java.util.List;
import org.checkerframework.checker.modifiability.grow.GrowChecker;
import org.checkerframework.checker.modifiability.iterator.IteratorChecker;
import org.checkerframework.checker.modifiability.replace.ReplaceChecker;
import org.checkerframework.checker.modifiability.seqgrow.SeqGrowChecker;
import org.checkerframework.checker.modifiability.shrink.ShrinkChecker;
import org.checkerframework.framework.source.AggregateChecker;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.source.SourceVisitor;

/**
 * A type-checker that warns, at compile time, if a program might throw {@link
 * UnsupportedOperationException} at run time due to calling a mutating method on a collection.
 *
 * <p>This is an aggregate checker that runs independent sub-checkers, one for each modifiability
 * capability and one for iterator modifiability preservation:
 *
 * <ul>
 *   <li>{@link GrowChecker} checks grow operations (e.g., {@code add}, {@code addAll})
 *   <li>{@link SeqGrowChecker} checks sequenced grow operations (e.g., {@code addFirst}, {@code
 *       addLast})
 *   <li>{@link ShrinkChecker} checks shrink operations (e.g., {@code remove}, {@code clear})
 *   <li>{@link ReplaceChecker} checks replace operations (e.g., {@code set}, {@code replaceAll})
 *   <li>{@link IteratorChecker} checks whether iterators preserve receiver modifiability
 * </ul>
 *
 * @checker_framework.manual #modifiability-checker Modifiability Checker
 */
public class ModifiabilityChecker extends AggregateChecker {

  /** Creates a ModifiabilityChecker. */
  public ModifiabilityChecker() {}

  @Override
  protected Collection<Class<? extends SourceChecker>> getSupportedCheckers() {
    return List.of(
        GrowChecker.class,
        SeqGrowChecker.class,
        ShrinkChecker.class,
        ReplaceChecker.class,
        IteratorChecker.class);
  }

  @Override
  protected SourceVisitor<?, ?> createSourceVisitor() {
    return new ModifiabilityVisitor(this);
  }
}
