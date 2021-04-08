package org.checkerframework.framework.testchecker.compound;

import java.util.LinkedHashSet;
import org.checkerframework.common.aliasing.AliasingChecker;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.value.ValueChecker;

public class AnotherCompoundChecker extends BaseTypeChecker {
  @Override
  protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
    // Make sure that options can be accessed by sub-checkers to determine
    // which subcheckers to run.
    @SuppressWarnings("unused")
    String option = super.getOption("nomsgtext");
    LinkedHashSet<Class<? extends BaseTypeChecker>> subcheckers = new LinkedHashSet<>();
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
