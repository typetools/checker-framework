import java.util.Date;
import java.util.List;
import java.util.regex.MatchResult;
import org.checkerframework.checker.regex.qual.Regex;

public class InapplicableTypes {

    // :: error: (type.invalid)
    @Regex int i;
    @Regex char c;

    // :: error: (type.invalid)
    @Regex Integer i2;
    @Regex Character c2;

    abstract static class MyMatchResult implements MatchResult {}

    @Regex MyMatchResult mp;

    @Regex String s;
    @Regex CharSequence cs;
    @Regex StringBuffer sb;

    // :: error: (type.invalid)
    @Regex List<Date> ls;
    // :: error: (type.invalid)
    List<@Regex Date> ld;
}
