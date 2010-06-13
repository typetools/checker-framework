import checkers.util.test.*;
import java.util.*;

public class GenericAliasInvalid {

    public static class SuperSetOne extends HashSet<@Odd Map<@Odd List<@Odd String>, @Odd String>> {

    }

    public void test() {
        //:: (assignment.type.incompatible)
        Set<Map<@Odd List<@Odd String>, @Odd String>> t = new SuperSetOne();
    }
}
