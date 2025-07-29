package org.checkerframework.checker.index.growonly;

import java.util.List;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.RelevantJavaTypes;
import org.checkerframework.framework.source.SuppressWarningsPrefix;

/**
 * A type-checker that enforces rules about mutable-length sequences.
 *
 * <p>This checker ensures that references annotated as {@code @GrowOnly} are not used to call
 * methods that can shrink the sequence (e.g., {@code remove()}, {@code clear()}). This allows other
 * checkers, like the Upper Bound Checker, to reason that indices for {@code @GrowOnly} collections
 * remain valid after mutations.
 *
 * @checker_framework.manual #growonly-checker Index Checker
 */
@RelevantJavaTypes({List.class})
@SuppressWarningsPrefix({"index", "mutable"})
public class GrowOnlyChecker extends BaseTypeChecker {
  /** Creates a new GrowOnlyChecker. */
  public GrowOnlyChecker() {}
}
