package com.sun.javadoc;
import checkers.nullness.quals.Nullable;

public abstract interface MemberDoc extends ProgramElementDoc {
  @Pure public abstract boolean isSynthetic();
}
