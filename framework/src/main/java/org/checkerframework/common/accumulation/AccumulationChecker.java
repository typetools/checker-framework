package org.checkerframework.common.accumulation;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.returnsreceiver.ReturnsReceiverChecker;

// TODO: This Javadoc comment should reference the Checker Framework manual, once the Accumulation
// Checker chapter is uncommented in the manual's LaTeX source.
/**
 * An accumulation checker is one that accumulates some property: method calls, map keys, etc.
 *
 * <p>This class provides a basic accumulation analysis that can be extended to implement an
 * accumulation type system. This accumulation analysis represents all facts as Strings.
 *
 * <p>This class supports modular alias analyses. To choose the alias analyses that your
 * accumulation checker uses, override the {@link #createAliasAnalyses()} method. By default, the
 * only alias analysis used is Returns Receiver.
 *
 * <p>The primary extension point is the constructor of {@link AccumulationAnnotatedTypeFactory},
 * which every subclass should override to provide custom annotations.
 */
public abstract class AccumulationChecker extends BaseTypeChecker {

  /** Set of alias analyses that are enabled in this particular accumulation checker. */
  private EnumSet<AliasAnalysis> aliasAnalyses;

  /** Constructs a new AccumulationChecker. */
  protected AccumulationChecker() {
    super();
    this.aliasAnalyses = createAliasAnalyses();
  }

  @Override
  protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
    LinkedHashSet<Class<? extends BaseTypeChecker>> checkers =
        super.getImmediateSubcheckerClasses();
    if (isEnabled(AliasAnalysis.RETURNS_RECEIVER)) {
      checkers.add(ReturnsReceiverChecker.class);
    }
    return checkers;
  }

  /**
   * The alias analyses that an accumulation checker can support. To add support for a new alias
   * analysis, add a new item to this enum and then implement any functionality of the checker
   * behind a call to {@link #isEnabled(AliasAnalysis)}.
   */
  public enum AliasAnalysis {
    /**
     * An alias analysis that detects methods that always return their own receiver (i.e. whose
     * return value and receiver are aliases).
     */
    RETURNS_RECEIVER
  }

  /**
   * Get the alias analyses that this checker should employ.
   *
   * @return the alias analyses
   */
  protected EnumSet<AliasAnalysis> createAliasAnalyses(
      @UnderInitialization AccumulationChecker this) {
    return EnumSet.of(AliasAnalysis.RETURNS_RECEIVER);
  }

  /**
   * Check whether the given alias analysis is enabled by this particular accumulation checker.
   *
   * @param aliasAnalysis the analysis to check
   * @return true iff the analysis is enabled
   */
  public boolean isEnabled(AliasAnalysis aliasAnalysis) {
    return aliasAnalyses.contains(aliasAnalysis);
  }
}
