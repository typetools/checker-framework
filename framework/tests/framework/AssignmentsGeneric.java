import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import testlib.util.*;

public class AssignmentsGeneric {

    private static final Map<
                    @Odd List<@Odd String>,
                    @Odd Map<@Odd Set<@Odd List<@Odd String>>, @Odd List<@Odd Set<@Odd String>>>>
            complex;

    static {
        complex =
                new HashMap<
                        @Odd List<@Odd String>,
                        @Odd Map<
                                @Odd Set<@Odd List<@Odd String>>,
                                @Odd List<@Odd Set<@Odd String>>>>();
    }

    public void testAssignment() {
        // :: warning: (cast.unsafe)
        @Odd String s = (@Odd String) "";

        List<@Odd String> lst = new LinkedList<>();
        lst = new ArrayList<@Odd String>();

        methodA(lst);
    }

    public void testEnhancedForLoop() {
        List<@Odd String> lst = new LinkedList<>();
        for (@Odd String str : lst) {
            System.out.println(str);
        }
    }

    public void testGenericInvocation() {
        List<@Odd String> lst = new LinkedList<>();
        // :: warning: (cast.unsafe)
        @Odd String s = (@Odd String) "";
        lst.add(s);
    }

    public List<@Odd String> testReturn() {
        return new LinkedList<@Odd String>();
    }

    /* ------------------------------------------------------------ */

    public void methodA(List<@Odd String> lst) {}
}
