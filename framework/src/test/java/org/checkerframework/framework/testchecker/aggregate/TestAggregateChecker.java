package org.checkerframework.framework.testchecker.aggregate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.checkerframework.common.aliasing.AliasingChecker;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.source.AggregateChecker;
import org.checkerframework.framework.source.SourceChecker;

/** Basic aggregate checker. */
public class TestAggregateChecker extends AggregateChecker {

    @Override
    protected Collection<Class<? extends SourceChecker>> getSupportedCheckers() {
        List<Class<? extends SourceChecker>> checkers = new ArrayList<>();
        checkers.add(ValueChecker.class);
        checkers.add(AliasingChecker.class);
        return checkers;
    }
}
