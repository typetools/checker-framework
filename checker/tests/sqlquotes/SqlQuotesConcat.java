import org.checkerframework.checker.sqlquotes.qual.SqlEvenQuotes;
import org.checkerframework.checker.sqlquotes.qual.SqlOddQuotes;
import org.checkerframework.checker.sqlquotes.qual.SqlQuotesUnknown;

public class SqlQuotesConcat {
  void oddOddConcat() {
    @SqlOddQuotes String odd1 = "asd''f'asdf''";
    @SqlOddQuotes String odd2 = "''asdf''''asdf'asdf'asdf'asdf";
    // :: error: (assignment)
    @SqlOddQuotes String oddResult1 = odd1 + odd2;
    @SqlEvenQuotes String evenResult1 = odd1 + odd2;
    // :: error: (compound.assignment)
    odd1 += odd2;
    // :: error: (compound.assignment)
    odd2 += odd1;

    @SqlOddQuotes String odd3 = "'asdf";
    @SqlOddQuotes String odd4 = "', ";
    // :: error: (assignment)
    @SqlOddQuotes String oddResult2 = odd3 + odd4;
    @SqlEvenQuotes String evenResult2 = odd3 + odd4;
    // :: error: (compound.assignment)
    odd3 += odd4;
    // :: error: (compound.assignment)
    odd4 += odd3;
  }

  void oddEvenConcat() {
    @SqlOddQuotes String odd1 = "asd''f'asdf''";
    @SqlEvenQuotes String even1 = "'''a'sdf";
    // :: error: (assignment)
    @SqlEvenQuotes String evenResult1 = odd1 + even1;
    @SqlOddQuotes String oddResult1 = odd1 + even1;
    odd1 += even1;
    // :: error: (compound.assignment)
    even1 += odd1;

    @SqlOddQuotes String odd2 = "'asdf";
    @SqlEvenQuotes String even2 = "', asdf '";
    // :: error: (assignment)
    @SqlEvenQuotes String evenResult2 = odd2 + even2;
    @SqlOddQuotes String oddResult2 = odd2 + even2;
    odd2 += even2;
    // :: error: (compound.assignment)
    even2 += odd2;
  }

  void evenOddConcat() {
    @SqlOddQuotes String odd1 = "asd''f'asdf''";
    @SqlEvenQuotes String even1 = "'''a'sdf";
    // :: error: (assignment)
    @SqlEvenQuotes String evenResult1 = even1 + odd1;
    @SqlOddQuotes String oddResult1 = even1 + odd1;
    odd1 += even1;
    // :: error: (compound.assignment)
    even1 += odd1;

    @SqlOddQuotes String odd2 = "'asdf";
    @SqlEvenQuotes String even2 = "', asdf '";
    // :: error: (assignment)
    @SqlEvenQuotes String evenResult2 = even2 + odd2;
    @SqlOddQuotes String oddResult2 = even2 + odd2;
    odd2 += even2;
    // :: error: (compound.assignment)
    even2 += odd2;
  }

  void evenEvenConcat() {
    @SqlEvenQuotes String even1 = "''''asdf''asd'f''asdf''''asdf'asdf''";
    @SqlEvenQuotes String even2 = "''asdf";
    // :: error: (assignment)
    @SqlOddQuotes String oddResult1 = even1 + even2;
    @SqlEvenQuotes String evenResult1 = even1 + even2;
    even1 += even2;
    even2 += even1;

    @SqlEvenQuotes String even3 = "'a''sdf'";
    @SqlEvenQuotes String even4 = "'asdf''asdf'asdf'asdf'''";
    // :: error: (assignment)
    @SqlOddQuotes String oddResult2 = even3 + even4;
    @SqlEvenQuotes String evenResult2 = even3 + even4;
    even3 += even4;
    even4 += even3;
  }

  void withTopConcat(@SqlQuotesUnknown String top) {
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
    // :: error: (compound.assignment)
    odd1 += top;
    top += odd1;

    @SqlEvenQuotes String even1 = "'a''sdf'";
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
    // :: error: (compound.assignment)
    even1 += top;
    top += even1;

    // :: error: (assignment)
    @SqlOddQuotes String oddResult5 = top + top;
    // :: error: (assignment)
    @SqlEvenQuotes String evenResult5 = top + top;
    String topResult5 = top + top;
    top += top;
  }
}
