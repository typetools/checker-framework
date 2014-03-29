import org.checkerframework.checker.guieffect.qual.UIType;

@UIType
public class BadUIOverrideChild extends SafeParent {
    // Should be an error b/c we marked this @UIType
    //:: error: (conflicts.override)
    @Override void m() {}
}
