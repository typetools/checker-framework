// A parenthesized array has the same length as the unparenthesized array.

import org.checkerframework.checker.index.qual.IndexFor;

public class ArrayAccessParenthesized {

  int[] field = new int[10];

  int local(int[] a, @IndexFor("#1") int i) {
    return (a)[i];
  }

  int field(@IndexFor("field") int i) {
    return (field)[i];
  }

  int thisField(@IndexFor("this.field") int i) {
    return (this.field)[i];
  }
}
