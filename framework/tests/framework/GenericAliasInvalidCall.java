import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import testlib.util.*;

public class GenericAliasInvalidCall {

    public static class SuperSetOne
            extends HashSet<@Odd Map<@Odd List<@Odd String>, @Odd String>> {}

    public void test() {
        Set<@Odd Map<@Odd List<@Odd String>, @Odd String>> s = new SuperSetOne();
        @Odd Map<List<@Odd String>, @Odd String> mapA =
                new @Odd HashMap<List<@Odd String>, @Odd String>();
        // :: error: (argument.type.incompatible)
        s.add(mapA);
    }
}
