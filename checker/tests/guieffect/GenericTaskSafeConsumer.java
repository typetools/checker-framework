import org.checkerframework.checker.guieffect.qual.AlwaysSafe;
import org.checkerframework.checker.guieffect.qual.SafeEffect;

public interface GenericTaskSafeConsumer {
  @SafeEffect
  public void runAsync(@AlwaysSafe IGenericTask t);
}
