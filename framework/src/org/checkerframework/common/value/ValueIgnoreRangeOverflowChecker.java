package org.checkerframework.common.value;

import java.util.Map;
import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.source.SupportedOptions;

/**
 * @author kelloggm
 * @checker_framework.manual #constant-value-checker Constant Value Checker
 */
@StubFiles("statically-executable.astub")
@SupportedOptions({ValueChecker.REPORT_EVAL_WARNS, ValueChecker.IGNORE_RANGE_OVERFLOW})
public class ValueIgnoreRangeOverflowChecker extends ValueChecker {

    @Override
    public Map<String, String> getOptions() {
        Map<String, String> options = super.getOptions();
        options.put(IGNORE_RANGE_OVERFLOW, null);
        return options;
    }
}
