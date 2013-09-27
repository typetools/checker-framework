package com.sun.javadoc;

import checkers.nullness.quals.Nullable;
import dataflow.quals.Pure;

public abstract interface MemberDoc extends ProgramElementDoc {
  @Pure public abstract boolean isSynthetic();
}
