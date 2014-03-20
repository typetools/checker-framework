import org.checkerframework.checker.guieffects.qual.SafeEffect;
import org.checkerframework.checker.guieffects.qual.UI;

public interface GenericTaskUIConsumer {
    @SafeEffect public void runAsync(@UI IGenericTask t);
}
