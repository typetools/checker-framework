package determinism;

import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

// @skip-test
class Issue8 {
    public static @PolyDet List<@PolyDet("use") String> copyStringList(
            @PolyDet List<@PolyDet("use") String> strings) {
        @PolyDet List<@PolyDet("use") String> result =
                (@PolyDet ArrayList<@PolyDet("use") String>)
                        new @PolyDet ArrayList<@PolyDet("use") String>();
        for (String s : strings) {
            // :: error: (argument.type.incompatible)
            result.add(s.trim());
        }
        return result;
    }
}
