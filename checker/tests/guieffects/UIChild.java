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
    //:: error: (conflicts.override)
    @Override @UIEffect public void doingSafeStuff() {}

    public void shouldNotBeUI() {
        //:: error: (call.invalid.ui)
        thingy.dangerous();
    }

    //:: error: (conflicts.annotations)
    @UIEffect @SafeEffect public void doubleAnnot1() {
    }
    //:: error: (conflicts.annotations)
    @UIEffect @PolyUIEffect public void doubleAnnot2() {
    }
    //:: error: (conflicts.annotations)
    @PolyUIEffect @SafeEffect public void doubleAnnot3() {
    }
}
