import tests.util.*;
import java.util.*;

public class GenericAlias {

    public static class SuperSetOne extends HashSet<@Odd Map<@Odd List<@Odd String>, @Odd String>> {

    }

    public void test() {
        Set<@Odd Map<@Odd List<@Odd String>, @Odd String>> s = new SuperSetOne();
        @Odd Map<@Odd List<@Odd String>, @Odd String> mapA =
            new @Odd HashMap<@Odd List<@Odd String>, @Odd String>();
        s.add(mapA);
    }

    public void regularGenerics() {
        Set<?> set = new HashSet<@Odd String>();
        Set<? extends Object> set2 = new HashSet<@Odd String>();
    }
}
