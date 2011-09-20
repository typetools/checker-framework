package java.util;
import checkers.igj.quals.*;

@I
public class Observable{
  public Observable(@AssignsFields Observable this) { throw new RuntimeException("skeleton method"); }
  public synchronized void addObserver(@Mutable Observable this, @Mutable Observer a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void deleteObserver(@Mutable Observable this, @ReadOnly Observer a1) { throw new RuntimeException("skeleton method"); }
  public void notifyObservers(@ReadOnly Observable this) { throw new RuntimeException("skeleton method"); }
  public void notifyObservers(@ReadOnly Observable this, @ReadOnly Object a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void deleteObservers(@Mutable Observable this) { throw new RuntimeException("skeleton method"); }
  public synchronized boolean hasChanged(@ReadOnly Observable this) { throw new RuntimeException("skeleton method"); }
  public synchronized int countObservers(@ReadOnly Observable this) { throw new RuntimeException("skeleton method"); }
}
