package java.util.regex;

import org.checkerframework.checker.lock.qual.*;

public class PatternSyntaxException extends IllegalArgumentException {
    private static final long serialVersionUID = -3864639126226059218L;
     public PatternSyntaxException(String desc, String regex, int index)  { throw new RuntimeException("skeleton method"); }
     public int getIndex(@GuardSatisfied PatternSyntaxException this) { throw new RuntimeException("skeleton method"); }
     public String getDescription(@GuardSatisfied PatternSyntaxException this) { throw new RuntimeException("skeleton method"); }
     public String getPattern(@GuardSatisfied PatternSyntaxException this) { throw new RuntimeException("skeleton method"); }
     public String getMessage(@GuardSatisfied PatternSyntaxException this) { throw new RuntimeException("skeleton method"); }
}
