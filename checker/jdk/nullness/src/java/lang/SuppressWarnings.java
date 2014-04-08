package java.lang;

import org.checkerframework.checker.nullness.qual.Nullable;


public abstract @interface SuppressWarnings{
  public abstract String[] value();
}
