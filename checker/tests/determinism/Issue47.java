import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class Issue47 {
    public <T extends @PolyDet("use") Object> @PolyDet("use") List<T> returnList1() {
        return null;
    }
}
