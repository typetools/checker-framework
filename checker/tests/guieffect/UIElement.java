import org.checkerframework.checker.guieffect.qual.SafeEffect;
import org.checkerframework.checker.guieffect.qual.UIType;

@UIType
public interface UIElement {
  public void dangerous();

  @SafeEffect
  public void repaint();

  @SafeEffect
  public void runOnUIThread(IAsyncUITask task);
}
