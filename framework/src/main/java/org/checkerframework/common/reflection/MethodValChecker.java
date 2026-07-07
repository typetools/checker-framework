package org.checkerframework.common.reflection;

import java.util.LinkedHashSet;
import java.util.Set;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.source.SourceChecker;
import org.plumelib.util.MapsP;

/**
 * The MethodVal Checker provides a sound estimate of the signature of Method objects.
 *
 * @checker_framework.manual #methodval-and-classval-checkers MethodVal Checker
 */
public class MethodValChecker extends BaseTypeChecker {
  @Override
  protected BaseTypeVisitor<?> createSourceVisitor() {
    return new MethodValVisitor(this);
  }

  @Override
  protected Set<Class<? extends SourceChecker>> getImmediateSubcheckerClasses() {
    // Don't call super otherwise MethodVal will be added as a subChecker
    // which creates a circular dependency.
    Set<Class<? extends SourceChecker>> subCheckers = new LinkedHashSet<>(MapsP.mapCapacity(2));
    subCheckers.add(ValueChecker.class);
    subCheckers.add(ClassValChecker.class);
    return subCheckers;
  }
}
