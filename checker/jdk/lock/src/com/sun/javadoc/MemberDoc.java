package com.sun.javadoc;

import org.checkerframework.checker.lock.qual.*;

public interface MemberDoc extends ProgramElementDoc {
   public abstract boolean isSynthetic(@GuardSatisfied MemberDoc this);
}
