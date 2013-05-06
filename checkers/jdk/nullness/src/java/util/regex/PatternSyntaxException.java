package java.util.regex;

public class PatternSyntaxException extends IllegalArgumentException {
    private static final long serialVersionUID = -3864639126226059218L;
    @SideEffectFree public PatternSyntaxException(String desc, String regex, int index)  { throw new RuntimeException("skeleton method"); }
    @Pure public int getIndex() { throw new RuntimeException("skeleton method"); }
    @Pure public String getDescription() { throw new RuntimeException("skeleton method"); }
    @Pure public String getPattern() { throw new RuntimeException("skeleton method"); }
    @SideEffectFree public String getMessage() { throw new RuntimeException("skeleton method"); }
}
