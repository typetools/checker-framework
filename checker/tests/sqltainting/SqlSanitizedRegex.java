import org.checkerframework.checker.sqltainting.qual.SqlSanitized;

public class SqlSanitizedRegex {

  void execute(@SqlSanitized String s) {}

  void emptyString() {
    execute("");
    execute(" ");
  }

  void alphanumericLiteral() {
    execute("0923uoiwej093NSDF9IJdjfs09");
    execute("8YUGKthbJU8yIUou8y7TYg");
    execute("09 23 uoiwej093N SDF9IJdjfs 09");
    execute("8 YUGKthbJU8y IUou 8   y7TYg");
  }

  void escapedLiterals() {
    execute("escaped \\\\ \\\\ backslashes");
    execute("escaped \\' \\\" quotes");
    execute("escaped \\- \\- hyphens");
    execute("escaped \\% \\% percents");
    execute("escaped \\_ \\_ underscores");

    execute("3 escaped \\'\\_\\% characters");
    execute("3 escaped \\\\\\-\\-\\\" characters");
  }

  void unescapedLiterals() {
    // :: error: (argument)
    execute("unescaped \\' \\\" \" _ \\\\ characters"); // error
    // :: error: (argument)
    execute("unescaped -- ' \\\\ characters"); // error
  }

  void otherChars() {
    // :: error: (argument)
    execute("109jnsdf;"); // error
    // :: error: (argument)
    execute("@293081$^&"); // error
  }
}
