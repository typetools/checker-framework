package org.checkerframework.framework.testchecker.compound;

import java.util.LinkedHashSet;
import java.util.Set;
import org.checkerframework.common.aliasing.AliasingChecker;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.SourceChecker;

/**
 * Used to test the compound checker design pattern. AliasingChecker and AnotherCompoundChecker are
 * subcheckers of this checker AnotherCompoundChecker relies on the Aliasing Checker, too. This is
 * so that the order of subcheckers is tested.
 */
public class CompoundChecker extends BaseTypeChecker {
  @Override
  protected Set<Class<? extends SourceChecker>> getImmediateSubcheckerClasses() {
    LinkedHashSet<Class<? extends SourceChecker>> subcheckers =
        new LinkedHashSet<>(super.getImmediateSubcheckerClasses());
    subcheckers.add(AliasingChecker.class);
    subcheckers.add(AnotherCompoundChecker.class);
    return subcheckers;
  }

  @Override
  protected BaseTypeVisitor<?> createSourceVisitor() {
    return new BaseTypeVisitor<CompoundCheckerAnnotatedTypeFactory>(this) {
      @Override
      protected CompoundCheckerAnnotatedTypeFactory createTypeFactory() {
        return new CompoundCheckerAnnotatedTypeFactory(checker);
      }
    };
  }
}
