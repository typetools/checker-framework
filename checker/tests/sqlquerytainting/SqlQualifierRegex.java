import org.checkerframework.checker.sqlquerytainting.qual.SqlEvenQuotes;
import org.checkerframework.checker.sqlquerytainting.qual.SqlOddQuotes;

public class SqlQualifierRegex {

  void oddQuotes() {
    // :: error: (assignment)
    @SqlOddQuotes String odd1 = "";

    @SqlEvenQuotes String even1 = "";
  }
}
