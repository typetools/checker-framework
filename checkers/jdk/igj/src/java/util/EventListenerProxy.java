package java.util;
import checkers.igj.quals.*;

@I
public abstract class EventListenerProxy implements EventListener {
  public EventListenerProxy(@I EventListener a1) { throw new RuntimeException("skeleton method"); }
  public @I EventListener getListener(@ReadOnly EventListenerProxy this) { throw new RuntimeException("skeleton method"); }
}
