package java.lang;

import checkers.nullness.quals.Nullable;


public abstract @interface SuppressWarnings{
  public abstract String[] value();
}
