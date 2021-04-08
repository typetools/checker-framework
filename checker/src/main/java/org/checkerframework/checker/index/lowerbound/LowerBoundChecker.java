package org.checkerframework.checker.index.lowerbound;

import java.util.HashSet;
import java.util.LinkedHashSet;
import org.checkerframework.checker.index.inequality.LessThanChecker;
import org.checkerframework.checker.index.searchindex.SearchIndexChecker;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.qual.RelevantJavaTypes;
import org.checkerframework.framework.source.SuppressWarningsPrefix;

/**
 * A type-checker for preventing fixed-length sequences such as arrays or strings from being
 * accessed with values that are too low. Normally bundled as part of the Index Checker.
 *
 * @checker_framework.manual #index-checker Index Checker
 */
@SuppressWarningsPrefix({"index", "lowerbound"})
@RelevantJavaTypes({
  Byte.class,
  Short.class,
  Integer.class,
  Long.class,
  Character.class,
  byte.class,
  short.class,
  int.class,
  long.class,
  char.class,
})
public class LowerBoundChecker extends BaseTypeChecker {

  /**
   * These collection classes have some subtypes whose length can change and some subtypes whose
   * length cannot change. Lower bound checker warnings are skipped at uses of them.
   */
  private HashSet<String> collectionBaseTypeNames;

  /**
   * A type-checker for preventing fixed-length sequences such as arrays or strings from being
   * accessed with values that are too low. Normally bundled as part of the Index Checker.
   */
  public LowerBoundChecker() {
    Class<?>[] collectionBaseClasses = {java.util.List.class, java.util.AbstractList.class};
    collectionBaseTypeNames = new HashSet<>(collectionBaseClasses.length);
    for (Class<?> collectionBaseClass : collectionBaseClasses) {
      collectionBaseTypeNames.add(collectionBaseClass.getName());
    }
  }

  @Override
  public boolean shouldSkipUses(@FullyQualifiedName String typeName) {
    if (collectionBaseTypeNames.contains(typeName)) {
      return true;
    }
    return super.shouldSkipUses(typeName);
  }

  @Override
  protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
    LinkedHashSet<Class<? extends BaseTypeChecker>> checkers =
        super.getImmediateSubcheckerClasses();
    checkers.add(ValueChecker.class);
    checkers.add(LessThanChecker.class);
    checkers.add(SearchIndexChecker.class);
    return checkers;
  }
}
