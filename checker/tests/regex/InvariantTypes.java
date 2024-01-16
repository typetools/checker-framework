import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.checkerframework.checker.regex.qual.*;

public class InvariantTypes {
  String[] sa = {"a"};
  String[] sa2 = {"a", "b"};
  public String[] sa3 = {"a", "b"};
  public static String[] sa4 = {"a", "b"};
  public final String[] sa5 = {"a", "b"};
  public static final String[] sa6 = {"a", "b"};
  final String[] sa7 = {"a", "b"};

  // tested above:  String[] sa = {"a"};
  @Regex String[] rsa = {"a"};
  String[] nrsa = {"(a"};
  // :: error: (array.initializer) :: error: (assignment)
  @Regex String[] rsaerr = {"(a"};

  List<String> ls = Arrays.asList("alice", "bob", "carol");
  List<@Regex String> lrs = Arrays.asList("alice", "bob", "carol");
  List<String> lnrs = Arrays.asList("(alice", "bob", "carol");
  // :: error: (type.arguments.not.inferred)
  List<@Regex String> lrserr = Arrays.asList("(alice", "bob", "carol");

  void unqm(String[] sa) {}

  void rem(@Regex String[] rsa) {}

  void recalls() {
    unqm(new String[] {"a"});
    // TODOINVARR:: error: (argument)
    unqm(new @Regex String[] {"a"});
    rem(new String[] {"a"});
    rem(new @Regex String[] {"a"});
  }

  void unqcalls() {
    unqm(new String[] {"a("});
    // TODOINVARR:: error: (argument)
    // :: error: (array.initializer)
    unqm(new @Regex String[] {"a("});
    // :: error: (argument)
    rem(new String[] {"a("});
    // :: error: (array.initializer)
    rem(new @Regex String[] {"a("});
  }

  // method argument context

  String[] retunqm(String[] sa) {
    return sa;
  }

  @Regex String[] retrem(@Regex String[] rsa) {
    return rsa;
  }

  @Regex String[] mixedm(String[] rsa) {
    return null;
  }

  void retunqcalls() {
    @Regex String[] re = mixedm(new String[] {"a("});
    // TODOINVARR:: error: (argument)
    String[] u = retunqm(new String[] {"a"});
    // TODOINVARR:: error: (argument)
    re = mixedm(new String[2]);
  }

  void lrem(List<@Regex String> p) {}

  void lunqm(List<String> p) {}

  void listcalls() {
    lunqm(Arrays.asList("alice", "bob", "carol"));
    lrem(Arrays.asList("alice", "bob", "carol"));
    lunqm(Arrays.asList("(alice", "bob", "carol"));
    // :: error: (type.arguments.not.inferred)
    lrem(Arrays.asList("(alice", "bob", "carol"));
  }

  class ReTests {
    ReTests(List<@Regex String> p) {}

    ReTests(List<String> p, int i) {}
  }

  void listctrs() {
    new ReTests(Arrays.asList("alice", "bob", "carol"), 0);
    new ReTests(Arrays.asList("alice", "bob", "carol"));
    new ReTests(Arrays.asList("(alice", "bob", "carol"), 0);
    // :: error: (type.arguments.not.inferred)
    new ReTests(Arrays.asList("(alice", "bob", "carol"));
  }

  <J> String join(final String delimiter, final Collection<J> objs) {
    return delimiter;
  }

  String s1 = join(" ", Arrays.asList("1", "2", "3"));
  String s2 = "xxx" + join(" ", Arrays.asList("1", "2", "3"));

  class TV<T> {
    List<List<T>> emptylist = Collections.emptyList();
  }
}
