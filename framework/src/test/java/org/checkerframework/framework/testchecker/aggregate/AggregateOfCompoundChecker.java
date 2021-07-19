package org.checkerframework.framework.testchecker.aggregate;

import java.util.Arrays;
import java.util.Collection;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.source.AggregateChecker;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.testchecker.compound.CompoundChecker;

/** An aggregate checker where one of the checkers is a compound checker. */
public class AggregateOfCompoundChecker extends AggregateChecker {

    @Override
    protected Collection<Class<? extends SourceChecker>> getSupportedCheckers() {
        return Arrays.asList(ValueChecker.class, CompoundChecker.class);
    }
}
