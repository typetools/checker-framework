import org.checkerframework.checker.guieffects.qual.SafeEffect;

// Should not inherit @UI!
public class WeakeningChild extends UIParent {
    // Should be valid to override @UI methods with @AlwaysSafe methods
    @Override @SafeEffect public void doingUIStuff() {}

    @Override public void doingSafeStuff() {}
}
