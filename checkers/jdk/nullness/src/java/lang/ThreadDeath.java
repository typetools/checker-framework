package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class ThreadDeath extends Error{
  public ThreadDeath() { throw new RuntimeException("skeleton method"); }
}
