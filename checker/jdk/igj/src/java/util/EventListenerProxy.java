package java.util;
import org.checkerframework.checker.igj.qual.*;

@I
public abstract class EventListenerProxy implements EventListener {
  public EventListenerProxy(@I EventListener a1) { throw new RuntimeException("skeleton method"); }
  public @I EventListener getListener(@ReadOnly EventListenerProxy this) { throw new RuntimeException("skeleton method"); }
}
