import org.checkerframework.checker.guieffect.qual.PolyUI;
import org.checkerframework.checker.guieffect.qual.PolyUIEffect;

@PolyUI public class GenericSubTask implements @PolyUI IGenericTask {
    public GenericTaskUIConsumer uicons;
    public GenericTaskSafeConsumer safecons;

    @Override
    @PolyUIEffect
    public void doGenericStuff() {
        // In here, it should be that this:@PolyUI
        uicons.runAsync(this); // should be okay
        // :: error: (argument.type.incompatible)
        safecons.runAsync(this); // should be error!
    }
}
