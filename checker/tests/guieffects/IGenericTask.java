import org.checkerframework.checker.guieffects.qual.PolyUI;
import org.checkerframework.checker.guieffects.qual.PolyUIEffect;
import org.checkerframework.checker.guieffects.qual.PolyUIType;

@PolyUIType
@PolyUI
public interface IGenericTask {
    @PolyUIEffect public void doGenericStuff();
}
