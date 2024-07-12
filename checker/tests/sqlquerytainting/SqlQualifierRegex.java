import org.checkerframework.checker.sqlquerytainting.qual.SqlEvenQuotes;
import org.checkerframework.checker.sqlquerytainting.qual.SqlOddQuotes;

public class SqlQualifierRegex {

  void oddQuotes() {
    // :: error: (argument)
    @SqlOddQuotes String odd1 = ""; // error

    @SqlEvenQuotes String even1 = "";
  }
}
