import org.checkerframework.checker.sqlquerytainting.qual.SqlEvenQuotes;
import org.checkerframework.checker.sqlquerytainting.qual.SqlOddQuotes;
import org.checkerframework.checker.sqlquerytainting.qual.SqlQueryUnknown;

public class SqlQualifierConcat {
  void oddOddConcat() {
    @SqlOddQuotes String odd1 = "asd\\'f'asdf''\\";
    @SqlOddQuotes String odd2 = "''asdf''''asdf'asdf'asdf'asdf";
    // :: error: (assignment)
    @SqlOddQuotes String oddResult1 = odd1 + odd2;
    @SqlEvenQuotes String evenResult1 = odd1 + odd2;

    @SqlOddQuotes String odd3 = "'asdf";
    @SqlOddQuotes String odd4 = "', ";
    // :: error: (assignment)
    @SqlOddQuotes String oddResult2 = odd3 + odd4;
    @SqlEvenQuotes String evenResult2 = odd3 + odd4;
  }

  void oddEvenConcat() {
    @SqlOddQuotes String odd1 = "asd\\'f'asdf''\\";
    @SqlEvenQuotes String even1 = "'\\''a\\sdf";
    // :: error: (assignment)
    @SqlEvenQuotes String evenResult1 = odd1 + even1;
    @SqlOddQuotes String oddResult1 = odd1 + even1;

    @SqlOddQuotes String odd2 = "'asdf";
    @SqlEvenQuotes String even2 = "', asdf '";
    // :: error: (assignment)
    @SqlEvenQuotes String evenResult2 = odd2 + even2;
    @SqlOddQuotes String oddResult2 = odd2 + even2;
  }

  void evenOddConcat() {
    @SqlOddQuotes String odd1 = "asd\\'f'asdf''\\";
    @SqlEvenQuotes String even1 = "'\\''a\\sdf";
    // :: error: (assignment)
    @SqlEvenQuotes String evenResult1 = even1 + odd1;
    @SqlOddQuotes String oddResult1 = even1 + odd1;

    @SqlOddQuotes String odd2 = "'asdf";
    @SqlEvenQuotes String even2 = "', asdf '";
    // :: error: (assignment)
    @SqlEvenQuotes String evenResult2 = even2 + odd2;
    @SqlOddQuotes String oddResult2 = even2 + odd2;
  }

  void evenEvenConcat() {
    @SqlEvenQuotes String even1 = "\\'''asdf''asd\\f'\\asdf'\\''asdf'asdf\\'";
    @SqlEvenQuotes String even2 = "''asdf";
    // :: error: (assignment)
    @SqlOddQuotes String oddResult1 = even1 + even2;
    @SqlEvenQuotes String evenResult1 = even1 + even2;

    @SqlEvenQuotes String even3 = "'a\\'sdf'";
    @SqlEvenQuotes String even4 = "'asdf''asdf'asdf'asdf'''";
    // :: error: (assignment)
    @SqlOddQuotes String oddResult2 = even3 + even4;
    @SqlEvenQuotes String evenResult2 = even3 + even4;
  }

  void withTopConcat(@SqlQueryUnknown String top) {
    @SqlOddQuotes String odd1 = "'asdf";
    // :: error: (assignment)
    @SqlOddQuotes String oddResult1 = odd1 + top;
    // :: error: (assignment)
    @SqlEvenQuotes String evenResult1 = odd1 + top;
    String topResult1 = odd1 + top;
    // :: error: (assignment)
    @SqlOddQuotes String oddResult2 = top + odd1;
    // :: error: (assignment)
    @SqlEvenQuotes String evenResult2 = top + odd1;
    String topResult2 = top + odd1;

    @SqlEvenQuotes String even1 = "'a\\'sdf'";
    // :: error: (assignment)
    @SqlOddQuotes String oddResult3 = even1 + top;
    // :: error: (assignment)
    @SqlEvenQuotes String evenResult3 = even1 + top;
    String topResult3 = even1 + top;
    // :: error: (assignment)
    @SqlOddQuotes String oddResult4 = top + even1;
    // :: error: (assignment)
    @SqlEvenQuotes String evenResult4 = top + even1;
    String topResult4 = top + even1;

    // :: error: (assignment)
    @SqlOddQuotes String oddResult5 = top + top;
    // :: error: (assignment)
    @SqlEvenQuotes String evenResult5 = top + top;
    String topResult5 = top + top;
  }
}
