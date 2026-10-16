package org.checkerframework.common.reflection;

import java.util.LinkedHashSet;
import java.util.Set;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.source.SourceChecker;
import org.plumelib.util.MapsP;

/**
 * The ClassVal Checker provides a sound estimate of the binary name of Class objects.
 *
 * @checker_framework.manual #methodval-and-classval-checkers ClassVal Checker
 */
public class ClassValChecker extends BaseTypeChecker {

  @Override
  protected BaseTypeVisitor<?> createSourceVisitor() {
    return new ClassValVisitor(this);
  }

  @Override
  protected Set<Class<? extends SourceChecker>> getImmediateSubcheckerClasses() {
    // Don't call super otherwise MethodVal will be added as a subChecker
    // which creates a circular dependency.
    Set<Class<? extends SourceChecker>> subCheckers = new LinkedHashSet<>(MapsP.mapCapacity(2));
    subCheckers.add(ValueChecker.class);
    return subCheckers;
  }

  @Override
  public boolean shouldResolveReflection() {
    // Because this checker is a subchecker of MethodVal,
    // reflection can't be resolved.
    return false;
  }
}
