import org.checkerframework.checker.javari.qual.*;
import java.awt.Point;

class Initializers {

    Point a = new Point(0, 1);
    @ReadOnly Point b = new Point(2, 3);
    @ReadOnly Point c = null;
    int i = 0;

}
