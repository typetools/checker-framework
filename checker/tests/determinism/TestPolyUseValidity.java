import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class TestPolyUseValidity {
    @PolyDet("use") int foo(@PolyDet int a) {
        return a;
    }
}
