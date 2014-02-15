import checkers.guieffects.quals.SafeEffect;
import checkers.guieffects.quals.UIType;

@UIType
public interface UIElement {
    public void dangerous();
    @SafeEffect public void repaint();
    @SafeEffect public void runOnUIThread(IAsyncUITask task);
}
