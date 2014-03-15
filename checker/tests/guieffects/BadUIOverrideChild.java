import org.checkerframework.checker.guieffects.qual.UIType;

@UIType
public class BadUIOverrideChild extends SafeParent {
    // Should be an error b/c we marked this @UIType
    //:: error: (conflicts.override)
    @Override void m() {}
}
