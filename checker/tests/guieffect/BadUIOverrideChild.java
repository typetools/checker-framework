import org.checkerframework.checker.guieffect.qual.UIType;

@UIType
public class BadUIOverrideChild extends SafeParent {
    // Should be an error because we marked this @UIType.
    @Override
    // :: error: (override.effect.invalid)
    void m() {}
}
