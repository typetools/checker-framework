package org.checkerframework.framework.testchecker.aggregate;

import java.util.LinkedHashSet;
import java.util.Set;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.source.AggregateChecker;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.testchecker.compound.CompoundChecker;

/** An aggregate checker where one of the checkers is a compound checker. */
public class AggregateOfCompoundChecker extends AggregateChecker {

  protected Set<Class<? extends SourceChecker>> getImmediateSubcheckerClasses() {
    LinkedHashSet<Class<? extends SourceChecker>> checkers =
        new LinkedHashSet<>(CollectionsPlume.mapCapacity(2));
    checkers.add(ValueChecker.class);
    checkers.add(CompoundChecker.class);
    return checkers;
  }
}
