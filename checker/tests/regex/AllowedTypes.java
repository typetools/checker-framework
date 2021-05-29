import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.text.Segment;
import org.checkerframework.checker.regex.qual.Regex;

public class AllowedTypes {
  @Regex CharSequence cs;
  @Regex String s11;
  @Regex StringBuilder sb;
  @Regex Segment s21;
  @Regex char c;
  @Regex Pattern p;
  @Regex Matcher m;
  @Regex Character c2;
  @Regex Object o;

  abstract static class MyMatchResult implements MatchResult {}

  @Regex MyMatchResult mp;

  // :: error: (anno.on.irrelevant)
  @Regex List<String> l;
  // :: error: (anno.on.irrelevant)
  ArrayList<@Regex Double> al;
  // :: error: (anno.on.irrelevant)
  @Regex int i;
  // :: error: (anno.on.irrelevant)
  @Regex boolean b;
  // :: error: (anno.on.irrelevant)
  @Regex Integer i2;

  void testAllowedTypes() {
    @Regex CharSequence cs;
    @Regex String s11;
    @Regex StringBuilder sb;
    @Regex Segment s21;
    @Regex char c;
    @Regex Object o;

    // :: error: (anno.on.irrelevant)
    @Regex List<String> l; // error
    // :: error: (anno.on.irrelevant)
    ArrayList<@Regex Double> al; // error
    // :: error: (anno.on.irrelevant)
    @Regex int i; // error
    // :: error: (anno.on.irrelevant)
    @Regex boolean b; // error

    @Regex String regex = "a";
    // :: error: (compound.assignment)
    regex += "(";

    String nonRegex = "a";
    nonRegex += "(";
  }
}
