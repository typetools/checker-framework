package jsr308.util;

import org.eclipse.core.runtime.jobs.*;

public class MutexSchedulingRule implements ISchedulingRule{
    public boolean isConflicting(ISchedulingRule rule){
        return rule == this;
    }

    public boolean contains(ISchedulingRule rule){
        return rule == this;
    }
}