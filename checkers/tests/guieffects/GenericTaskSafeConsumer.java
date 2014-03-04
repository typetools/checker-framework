import checkers.guieffects.quals.AlwaysSafe;
import checkers.guieffects.quals.SafeEffect;

public interface GenericTaskSafeConsumer {
    @SafeEffect public void runAsync(@AlwaysSafe IGenericTask t);
}
