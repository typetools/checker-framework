import org.checkerframework.checker.guieffects.qual.SafeEffect;
import org.checkerframework.checker.guieffects.qual.UIType;

@UIType
public class UIParent {
    protected UIElement thingy;

    @SafeEffect // Making this ctor safe to allow easy safe subclasses
    public UIParent() { }

    public void doingUIStuff() { thingy.dangerous(); } // should have UI effect

    @SafeEffect
    public void doingSafeStuff() {} // non-UI
}
