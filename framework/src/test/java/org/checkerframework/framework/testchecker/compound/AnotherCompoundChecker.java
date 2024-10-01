package org.checkerframework.framework.testchecker.compound;

import java.util.LinkedHashSet;
import java.util.Set;
import org.checkerframework.common.aliasing.AliasingChecker;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.source.SourceChecker;

public class AnotherCompoundChecker extends BaseTypeChecker {
  @Override
  protected Set<Class<? extends SourceChecker>> getImmediateSubcheckerClasses() {
    // Make sure that options can be accessed by sub-checkers to determine
    // which subcheckers to run.
    @SuppressWarnings("unused")
    String option = super.getOption("nomsgtext");
    LinkedHashSet<Class<? extends SourceChecker>> subcheckers = new LinkedHashSet<>();
    subcheckers.addAll(super.getImmediateSubcheckerClasses());
    subcheckers.add(AliasingChecker.class);
    subcheckers.add(ValueChecker.class);
    return subcheckers;
  }

  @Override
  protected BaseTypeVisitor<?> createSourceVisitor() {
    return new BaseTypeVisitor<AnotherCompoundCheckerAnnotatedTypeFactory>(this) {
      @Override
      protected AnotherCompoundCheckerAnnotatedTypeFactory createTypeFactory() {
        return new AnotherCompoundCheckerAnnotatedTypeFactory(checker);
      }
    };
  }
}
