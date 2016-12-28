package java.util;


public class Observable{
  public Observable() { throw new RuntimeException("skeleton method"); }
  public synchronized void addObserver(Observer a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void deleteObserver(Observer a1) { throw new RuntimeException("skeleton method"); }
  public void notifyObservers() { throw new RuntimeException("skeleton method"); }
  public void notifyObservers(Object a1) { throw new RuntimeException("skeleton method"); }
  public synchronized void deleteObservers() { throw new RuntimeException("skeleton method"); }
  public synchronized boolean hasChanged() { throw new RuntimeException("skeleton method"); }
  public synchronized int countObservers() { throw new RuntimeException("skeleton method"); }
}
