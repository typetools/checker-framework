package java.util.regex;

import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.checker.lock.qual.*;

public class PatternSyntaxException extends IllegalArgumentException {
    private static final long serialVersionUID = -3864639126226059218L;
    @SideEffectFree public PatternSyntaxException(String desc, String regex, int index)  { throw new RuntimeException("skeleton method"); }
    @Pure public int getIndex(@GuardSatisfied PatternSyntaxException this) { throw new RuntimeException("skeleton method"); }
    @Pure public String getDescription(@GuardSatisfied PatternSyntaxException this) { throw new RuntimeException("skeleton method"); }
    @Pure public String getPattern(@GuardSatisfied PatternSyntaxException this) { throw new RuntimeException("skeleton method"); }
    @Pure public String getMessage(@GuardSatisfied PatternSyntaxException this) { throw new RuntimeException("skeleton method"); }
}
