package org.checkerframework.eclipse.util;

import org.eclipse.core.runtime.jobs.*;

public class MutexSchedulingRule implements ISchedulingRule {
    @Override
    public boolean isConflicting(ISchedulingRule rule) {
        return rule == this;
    }

    @Override
    public boolean contains(ISchedulingRule rule) {
        return rule == this;
    }
}
