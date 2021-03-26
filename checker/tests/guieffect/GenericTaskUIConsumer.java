import org.checkerframework.checker.guieffect.qual.SafeEffect;
import org.checkerframework.checker.guieffect.qual.UI;

public interface GenericTaskUIConsumer {
  @SafeEffect
  public void runAsync(@UI IGenericTask t);
}
