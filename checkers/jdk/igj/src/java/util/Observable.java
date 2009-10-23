package java.util;
import checkers.igj.quals.*;

@I
public class Observable{
  public Observable() @AssignsFields { throw new RuntimeException("skeleton method"); }
  public synchronized void addObserver(@Mutable java.util.Observer a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public synchronized void deleteObserver(@ReadOnly java.util.Observer a1) @Mutable { throw new RuntimeException("skeleton method"); }
  public void notifyObservers() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public void notifyObservers(@ReadOnly java.lang.Object a1) @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized void deleteObservers() @Mutable { throw new RuntimeException("skeleton method"); }
  public synchronized boolean hasChanged() @ReadOnly { throw new RuntimeException("skeleton method"); }
  public synchronized int countObservers() @ReadOnly { throw new RuntimeException("skeleton method"); }
}
