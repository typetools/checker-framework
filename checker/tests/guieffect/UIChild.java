import org.checkerframework.checker.guieffect.qual.PolyUIEffect;
import org.checkerframework.checker.guieffect.qual.SafeEffect;
import org.checkerframework.checker.guieffect.qual.UIEffect;

// Should not inherit @UI!
public class UIChild extends UIParent {
    @Override public void doingUIStuff() {
        //:: error: (call.invalid.ui)
        thingy.dangerous();
    }

    // Should be an error to make this @UI
    //:: error: (override.effect.invalid)
    @Override @UIEffect public void doingSafeStuff() {}

    public void shouldNotBeUI() {
        //:: error: (call.invalid.ui)
        thingy.dangerous();
    }

    //:: error: (annotations.conflicts)
    @UIEffect @SafeEffect public void doubleAnnot1() {
    }
    //:: error: (annotations.conflicts)
    @UIEffect @PolyUIEffect public void doubleAnnot2() {
    }
    //:: error: (annotations.conflicts)
    @PolyUIEffect @SafeEffect public void doubleAnnot3() {
    }
}
