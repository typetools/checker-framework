import org.checkerframework.checker.guieffects.qual.AlwaysSafe;
import org.checkerframework.checker.guieffects.qual.SafeEffect;

public interface GenericTaskSafeConsumer {
    @SafeEffect public void runAsync(@AlwaysSafe IGenericTask t);
}
