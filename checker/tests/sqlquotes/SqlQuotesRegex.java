import org.checkerframework.checker.sqlquotes.qual.SqlEvenQuotes;
import org.checkerframework.checker.sqlquotes.qual.SqlOddQuotes;

public class SqlQuotesRegex {

  void oddNoEscaped() {
    // :: error: (assignment)
    @SqlOddQuotes String none = "asdf";
    @SqlOddQuotes String one = "asdf'asdf";
    // :: error: (assignment)
    @SqlOddQuotes String two = "'asdf'";
    @SqlOddQuotes String three = "'asdf'asdf'";
    // :: error: (assignment)
    @SqlOddQuotes String manyEven = "'asdf''asdf'asdf'asdf'''";
    @SqlOddQuotes String manyOdd = "''asdf'asdf'''asdf'asdf''";
  }

  void oddWithBackslashes() {
    // :: error: (assignment)
    @SqlOddQuotes String none = "asdf\\'";
    @SqlOddQuotes String one = "asdf\\''asdf";
    // :: error: (assignment)
    @SqlOddQuotes String two = "'a\\'sdf'";
    @SqlOddQuotes String three = "\\''asdf'\\asdf'";
    // :: error: (assignment)
    @SqlOddQuotes String manyEven = "'as\\'df'\\''a\\sdf'asdf'as\\'\\'df'\\'''";
    @SqlOddQuotes String manyOdd = "'\\''a\\sdf'asdf'''as\\df'as\\'df''\\'";
  }

  void evenNoEscaped() {
    @SqlEvenQuotes String none = "";
    // :: error: (assignment)
    @SqlEvenQuotes String one = "'asdf";
    @SqlEvenQuotes String two = "''asdf";
    // :: error: (assignment)
    @SqlEvenQuotes String three = "asdf'asdf''";
    @SqlEvenQuotes String manyEven = "''asdf''asdf'asdf''asdf'asdf";
    // :: error: (assignment)
    @SqlEvenQuotes String manyOdd = "asdf''''asdf'asdf'asdf'asdf";
  }

  void evenWithBackslashes() {
    @SqlEvenQuotes String none = "\\'\\'\\'";
    // :: error: (assignment)
    @SqlEvenQuotes String one = "'asdf\\'";
    @SqlEvenQuotes String two = "'\\''a\\sdf";
    // :: error: (assignment)
    @SqlEvenQuotes String three = "asd\\'f'asdf''";
    @SqlEvenQuotes String manyEven = "\\'''asdf''asd\\f'\\asdf'\\''asdf'asdf\\'";
    // :: error: (assignment)
    @SqlEvenQuotes String manyOdd = "asdf''\\'''\\asdf'asdf'a\\'sdf'\\'asdf";
  }
}
