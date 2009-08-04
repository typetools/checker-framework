package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public class Observable{
  public Observable() { throw new RuntimeException("skeleton method"); }
  public synchronized void addObserver(java.util.Observer a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void deleteObserver(@Nullable java.util.Observer a1) { throw new RuntimeException("skeleton method"); }
  public void notifyObservers() { throw new RuntimeException("skeleton method"); }
  public void notifyObservers(@Nullable java.lang.Object a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void deleteObservers() { throw new RuntimeException("skeleton method"); }
  public synchronized boolean hasChanged() { throw new RuntimeException("skeleton method"); }
  public synchronized int countObservers() { throw new RuntimeException("skeleton method"); }
}
