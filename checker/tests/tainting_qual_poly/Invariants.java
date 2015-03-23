

import java.util.*;
import org.checkerframework.checker.tainting.qual.*;
import org.checkerframework.qualframework.poly.qual.*;

public class Invariants {

    List<String> ls = Arrays.asList("alice", "bob", "carol");
    List<@Untainted String> lrs = Arrays.asList("alice", "bob", "carol");
    List<Integer> lnrs = Arrays.asList(1, 2, 3);
    //:: error: (assignment.type.incompatible)
    List<@Untainted Integer> lrserr = Arrays.asList(1);
    @Untainted Integer i;
    List<@Untainted Integer> lrsno = Arrays.asList(i);
}