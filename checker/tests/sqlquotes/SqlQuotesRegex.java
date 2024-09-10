import org.checkerframework.checker.sqlquotes.qual.SqlEvenQuotes;
import org.checkerframework.checker.sqlquotes.qual.SqlOddQuotes;

public class SqlQuotesRegex {

  void oddQuotes() {
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

  void evenQuotes() {
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
}
