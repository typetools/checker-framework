import tests.util.*;
import java.util.*;

public class GenericAliasInvalid {

    public static class SuperSetOne extends HashSet<@Odd Map<@Odd List<@Odd String>, @Odd String>> {

    }

    public void test() {
        //:: error: (assignment.type.incompatible)
        Set<Map<@Odd List<@Odd String>, @Odd String>> t = new SuperSetOne();
    }
}
