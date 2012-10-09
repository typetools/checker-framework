import tests.util.*;
import java.util.*;

public class AssignmentsGeneric {

    private static final Map<@Odd List<@Odd String>, @Odd Map<@Odd Set<@Odd List<@Odd String>>, @Odd List<@Odd Set<@Odd String>>>> complex;

    static {
        complex = new HashMap<@Odd List<@Odd String>,
                @Odd Map<@Odd Set<@Odd List<@Odd String>>,
                          @Odd List<@Odd Set<@Odd String>>>>();
    }

    public void testAssignment() {
        //:: warning: (cast.unsafe)
        @Odd String s = (@Odd String)"";

        List<@Odd String> lst = new LinkedList<@Odd String>();
        lst = new ArrayList<@Odd String>();

        methodA(lst);
    }

    public void testEnhancedForLoop() {
        List<@Odd String> lst = new LinkedList<@Odd String>();
        for (@Odd String str : lst) {
            System.out.println(str);
        }
    }

    public void testGenericInvocation() {
        List<@Odd String> lst = new LinkedList<@Odd String>();
        //:: warning: (cast.unsafe)
        @Odd String s = (@Odd String)"";
        lst.add(s);
    }


    public List<@Odd String> testReturn() {
        return new LinkedList<@Odd String>();
    }

    /* ------------------------------------------------------------ */

    public void methodA(List<@Odd String> lst) {}
}
