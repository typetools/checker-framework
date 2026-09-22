package org.checkerframework.checker.modifiability;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.modifiability.iterator.IteratorChecker;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.source.SourceChecker;

/**
 * Base class for the Modifiability sub-checkers.
 *
 * <p>This class exists so that the sub-checkers share two things:
 *
 * <ul>
 *   <li>The {@code messages.properties} file in {@code org.checkerframework.checker.modifiability},
 *       which {@link org.checkerframework.framework.source.SourceChecker#getMessagesProperties()}
 *       finds via this class. The Grow, SeqGrow, Shrink, and Replace checkers all report
 *       diagnostics whose message keys are defined there; without this common superclass, those
 *       keys would need to be duplicated in each sub-checker's package.
 *   <li>The stub files in {@code org.checkerframework.checker.modifiability}; see {@link
 *       #getExtraStubFiles}.
 * </ul>
 */
public abstract class ModifiabilityBaseChecker extends BaseTypeChecker {

  /**
   * The stub files that all the Modifiability sub-checkers use, as absolute resource paths.
   *
   * <p>A {@code @}{@link org.checkerframework.framework.qual.StubFiles} annotation cannot be used
   * for them, because it is not inherited and its stub files must be in the same directory as the
   * checker class that is annotated -- which is what would force one copy of each file per
   * sub-checker package.
   */
  private static final List<String> SHARED_STUB_FILES =
      List.of(
          "/org/checkerframework/checker/modifiability/ical4j.astub",
          "/org/checkerframework/checker/modifiability/javaparser.astub",
          "/org/checkerframework/checker/modifiability/jdk.astub");

  /** Creates a new ModifiabilityBaseChecker. */
  protected ModifiabilityBaseChecker() {}

  /**
   * {@inheritDoc}
   *
   * <p>This implementation adds the stub files that the sub-checkers share. They are written in
   * terms of the capability qualifiers such as {@code @Modifiable}; the Iterator Checker parses
   * them too, but ignores every qualifier that is not in its own hierarchy.
   */
  @Override
  public List<String> getExtraStubFiles() {
    List<String> result = new ArrayList<>(super.getExtraStubFiles());
    result.addAll(SHARED_STUB_FILES);
    return result;
  }

  /**
   * Returns true if this checker refines the result of {@code iterator()} and {@code
   * listIterator()}, and therefore needs to read the Iterator Checker's qualifiers. The Iterator
   * Checker itself does not (it would be its own subchecker), and neither does the SeqGrow Checker,
   * because an iterator has no sequenced-grow methods.
   *
   * @return true if this checker needs the Iterator Checker as a subchecker
   */
  protected boolean usesIteratorChecker() {
    return true;
  }

  @Override
  protected Set<Class<? extends SourceChecker>> getImmediateSubcheckerClasses() {
    Set<Class<? extends SourceChecker>> checkers = super.getImmediateSubcheckerClasses();
    if (usesIteratorChecker()) {
      checkers.add(IteratorChecker.class);
    }
    return checkers;
  }
}
