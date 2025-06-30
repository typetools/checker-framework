package org.checkerframework.checker.index.mutable;

import java.util.ArrayList;
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
 * @checker_framework.manual #index-checker-mutable-length Support for mutable-length sequences
 */
@RelevantJavaTypes({List.class, ArrayList.class
  /** LinkedList.class, Vector.class, Stack.class, AbstractList.class, CopyOnWriteArrayList.class */
})
@SuppressWarningsPrefix({"index", "mutable"})
public class MutableIndexChecker extends BaseTypeChecker {
  /* Creates a new MutableIndexChecker. */
  public MutableIndexChecker() {}
}
