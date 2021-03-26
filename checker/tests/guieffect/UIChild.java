import org.checkerframework.checker.guieffect.qual.PolyUIEffect;
import org.checkerframework.checker.guieffect.qual.SafeEffect;
import org.checkerframework.checker.guieffect.qual.UIEffect;

// Should not inherit @UI!
public class UIChild extends UIParent {
  @Override
  public void doingUIStuff() {
    // :: error: (call.invalid.ui)
    thingy.dangerous();
  }

  // Should be an error to make this @UI
  @Override
  @UIEffect
  // :: error: (override.effect.invalid)
  public void doingSafeStuff() {}

  public void shouldNotBeUI() {
    // :: error: (call.invalid.ui)
    thingy.dangerous();
  }

  @UIEffect
  @SafeEffect
  // :: error: (annotations.conflicts)
  public void doubleAnnot1() {}

  @UIEffect
  @PolyUIEffect
  // :: error: (annotations.conflicts) :: error: (polymorphism.invalid)
  public void doubleAnnot2() {}

  @PolyUIEffect
  @SafeEffect
  // :: error: (annotations.conflicts) :: error: (polymorphism.invalid)
  public void doubleAnnot3() {}
}
