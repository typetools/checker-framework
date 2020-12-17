import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import org.checkerframework.checker.guieffect.qual.UIType;

// Test the stub file handling
@UIType
public class MouseTest extends MouseAdapter {
    @Override
    public void mouseEntered(MouseEvent arg0) {
        IAsyncUITask t = null;
        t.doStuff();
    }
}
