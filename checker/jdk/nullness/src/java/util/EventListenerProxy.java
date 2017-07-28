package java.util;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class EventListenerProxy<T extends EventListener> implements EventListener {
  public EventListenerProxy(T a1) { throw new RuntimeException("skeleton method"); }
  public EventListener getListener() { throw new RuntimeException("skeleton method"); }
}
