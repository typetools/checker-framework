package java.util;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Observable{
  public Observable() { throw new RuntimeException("skeleton method"); }
  public synchronized void addObserver(Observer a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void deleteObserver(@Nullable Observer a1) { throw new RuntimeException("skeleton method"); }
  public void notifyObservers() { throw new RuntimeException("skeleton method"); }
  public void notifyObservers(@Nullable Object a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void deleteObservers() { throw new RuntimeException("skeleton method"); }
  public synchronized boolean hasChanged() { throw new RuntimeException("skeleton method"); }
  public synchronized int countObservers() { throw new RuntimeException("skeleton method"); }
}
