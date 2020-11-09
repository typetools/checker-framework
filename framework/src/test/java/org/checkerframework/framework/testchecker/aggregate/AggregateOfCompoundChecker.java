package org.checkerframework.framework.testchecker.aggregate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.source.AggregateChecker;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.testchecker.compound.CompoundChecker;

/** An aggregate checker where one of the checkers is a compound checker. */
public class AggregateOfCompoundChecker extends AggregateChecker {

    @Override
    protected Collection<Class<? extends SourceChecker>> getSupportedCheckers() {
        List<Class<? extends SourceChecker>> checkers = new ArrayList<>();
        checkers.add(ValueChecker.class);
        checkers.add(CompoundChecker.class);
        return checkers;
    }
}
