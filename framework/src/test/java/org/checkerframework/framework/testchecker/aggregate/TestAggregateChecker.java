package org.checkerframework.framework.testchecker.aggregate;

import java.util.LinkedHashSet;
import java.util.Set;
import org.checkerframework.common.aliasing.AliasingChecker;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.source.AggregateChecker;
import org.checkerframework.framework.source.SourceChecker;

/** Basic aggregate checker. */
public class TestAggregateChecker extends AggregateChecker {

  protected Set<Class<? extends SourceChecker>> getImmediateSubcheckerClasses() {
    Set<Class<? extends SourceChecker>> checkers =
        new LinkedHashSet<>(CollectionsPlume.mapCapacity(2));
    checkers.add(ValueChecker.class);
    checkers.add(AliasingChecker.class);
    return checkers;
  }
}
