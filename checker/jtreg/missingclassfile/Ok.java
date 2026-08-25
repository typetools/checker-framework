import lib.Box;
import lib.MemberOnly;

/**
 * The variants that do not crash, which pin down which ingredients the crash requires: the absent
 * class must appear in a type argument of the <em>supertype</em> of a classpath class that the
 * checked source uses as a type. See Issue8055.java.
 */
public class Ok {

  // The absent class appears only in this class's member signatures, which are never looked up.
  MemberOnly memberOnlyField;

  Object memberOnlyClassLiteral() {
    return MemberOnly.class;
  }

  Object memberOnlyCreation() {
    return new MemberOnly();
  }

  // The generic supertype itself does not mention the absent class.
  Box<String> boxField;

  Object boxClassLiteral() {
    return Box.class;
  }
}
