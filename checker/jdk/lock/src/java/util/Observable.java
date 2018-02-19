package java.util;


import org.checkerframework.checker.lock.qual.GuardSatisfied;

public class Observable{
  public Observable() { throw new RuntimeException("skeleton method"); }
  public synchronized void addObserver(@GuardSatisfied Observable this, Observer a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void deleteObserver(@GuardSatisfied Observable this, Observer a1) { throw new RuntimeException("skeleton method"); }
  public void notifyObservers() { throw new RuntimeException("skeleton method"); }
  public void notifyObservers(Object a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void deleteObservers(@GuardSatisfied Observable this) { throw new RuntimeException("skeleton method"); }
  public synchronized boolean hasChanged() { throw new RuntimeException("skeleton method"); }
  public synchronized int countObservers() { throw new RuntimeException("skeleton method"); }
}
