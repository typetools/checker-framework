import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.checkerframework.framework.testchecker.util.*;

public class GenericAlias {

    public static class SuperSetOne
            extends HashSet<@Odd Map<@Odd List<@Odd String>, @Odd String>> {}

    public void test() {
        Set<@Odd Map<@Odd List<@Odd String>, @Odd String>> s = new SuperSetOne();
        @Odd Map<@Odd List<@Odd String>, @Odd String> mapA =
                // :: warning: (cast.unsafe.constructor.invocation)
                new @Odd HashMap<@Odd List<@Odd String>, @Odd String>();
        s.add(mapA);
    }

    public void regularGenerics() {
        Set<?> set = new HashSet<@Odd String>();
        Set<? extends Object> set2 = new HashSet<@Odd String>();
    }
}
