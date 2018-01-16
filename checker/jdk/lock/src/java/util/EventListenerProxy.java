package java.util;

public abstract class EventListenerProxy<T extends EventListener> implements EventListener {
  public EventListenerProxy(T a1) { throw new RuntimeException("skeleton method"); }
  public EventListener getListener(@GuardSatisfied EventListenerProxy<T> this) { throw new RuntimeException("skeleton method"); }
}
