import checkers.guieffects.quals.SafeEffect;
import checkers.guieffects.quals.UI;

public interface GenericTaskUIConsumer {
    @SafeEffect public void runAsync(@UI IGenericTask t);
}
