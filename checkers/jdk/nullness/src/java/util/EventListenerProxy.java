package java.util;
import checkers.nullness.quals.*;
@checkers.quals.DefaultQualifier("checkers.nullness.quals.NonNull")

public abstract class EventListenerProxy implements EventListener {
  public EventListenerProxy(EventListener a1) { throw new RuntimeException("skeleton method"); }
  public EventListener getListener() { throw new RuntimeException("skeleton method"); }
}
