package tests;

// Test case for Issue 343
// https://code.google.com/p/checker-framework/issues/detail?id=343

import org.checkerframework.checker.fenum.FenumChecker;
import org.checkerframework.checker.regex.RegexChecker;
import org.checkerframework.framework.source.AggregateChecker;
import org.checkerframework.framework.source.SourceChecker;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class NestedAggregateChecker extends AggregateChecker {
    /*@Override
    protected Collection<Class<? extends SourceChecker>> getSupportedCheckers() {
        return (List<Class<? extends SourceChecker>>) Arrays.asList(
                FenumChecker.class,
                RegexChecker.class
        );
    }*/
}
