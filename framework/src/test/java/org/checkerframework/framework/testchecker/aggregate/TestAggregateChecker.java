package org.checkerframework.framework.testchecker.aggregate;

import java.util.Arrays;
import java.util.Collection;
import org.checkerframework.common.aliasing.AliasingChecker;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.source.AggregateChecker;
import org.checkerframework.framework.source.SourceChecker;

/** Basic aggregate checker. */
public class TestAggregateChecker extends AggregateChecker {

    @Override
    protected Collection<Class<? extends SourceChecker>> getSupportedCheckers() {
        return Arrays.asList(ValueChecker.class, AliasingChecker.class);
    }
}
