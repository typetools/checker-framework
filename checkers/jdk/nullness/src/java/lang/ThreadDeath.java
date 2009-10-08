package java.lang;

import checkers.nullness.quals.*;

@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class ThreadDeath{
  public ThreadDeath() { throw new RuntimeException("skeleton method"); }
}
