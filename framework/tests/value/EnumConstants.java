// A test for the @EnumVal annotation.

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.checkerframework.common.value.qual.*;

public class EnumConstants {
  enum MyEnum {
    VALUE,
    OTHER_VALUE,
    THIRD_VALUE
  }

  static void subtyping1(@EnumVal("VALUE") MyEnum value) {
    @EnumVal("VALUE") MyEnum value2 = value;
    // :: error: (assignment)
    @EnumVal("OTHER_VALUE") MyEnum value3 = value;
    @UnknownVal MyEnum value4 = value;
    @EnumVal({"VALUE", "OTHER_VALUE"}) MyEnum value5 = value;
  }

  static void subtyping2(@EnumVal({"VALUE", "OTHER_VALUE"}) MyEnum value) {
    // :: error: (assignment)
    @EnumVal("VALUE") MyEnum value2 = value;
    // :: error: (assignment)
    @EnumVal("OTHER_VALUE") MyEnum value3 = value;
    @UnknownVal MyEnum value4 = value;
    @EnumVal({"VALUE", "OTHER_VALUE"}) MyEnum value5 = value;
    @EnumVal({"VALUE", "OTHER_VALUE", "THIRD_VALUE"}) MyEnum value6 = value;
  }

  static void enumConstants() {
    @EnumVal("VALUE") MyEnum v1 = MyEnum.VALUE;
    @EnumVal({"VALUE", "OTHER_VALUE"}) MyEnum v2 = MyEnum.VALUE;
    // :: error: (assignment)
    @EnumVal("OTHER_VALUE") MyEnum v3 = MyEnum.VALUE;
  }

  static void enumToString() {
    @EnumVal("VALUE") MyEnum v1 = MyEnum.VALUE;
    // NOT toString(), because programmers can override that. .name() is final.
    @StringVal("VALUE") String s1 = v1.name();
  }

  // These are just paranoia based on the implementation strategy for enum constant defaulting.
  static void nonConstantEnum(MyEnum m) {
    // :: error: (assignment)
    @EnumVal("m") MyEnum m2 = m;
    // :: error: (assignment)
    @EnumVal("m3") MyEnum m3 = m;
  }

  static void enums(@EnumVal("VALUE") MyEnum... enums) {}

  static void testEnums() {
    enums();
    enums(MyEnum.VALUE);
    // :: error: (argument)
    enums(MyEnum.OTHER_VALUE);
  }

  static void testEnumArraysInConditional(boolean append, String filename) throws IOException {
    Files.newBufferedWriter(
        Paths.get(filename),
        UTF_8,
        append ? new StandardOpenOption[] {CREATE, APPEND} : new StandardOpenOption[] {CREATE});
  }

  public static String unescapeJava(String orig, char c) {
    StringBuilder sb = new StringBuilder();
    // The previous escape character was seen just before this position.
    int postEsc = 0;
    int thisEsc = 0; // orig.indexOf('\\');
    while (thisEsc != -1) {
      switch (c) {
        case 'n':
          sb.append(orig.substring(postEsc, thisEsc));
          sb.append('\n'); // not lineSep
          postEsc = thisEsc + 2;
          break;
        case 'r':
          sb.append(orig.substring(postEsc, thisEsc));
          sb.append('\r');
          postEsc = thisEsc + 2;
          break;
        case 't':
          sb.append(orig.substring(postEsc, thisEsc));
          sb.append('\t');
          postEsc = thisEsc + 2;
          break;
        case '\\':
          // This is not in the default case because the search would find
          // the quoted backslash.  Here we include the first backslash in
          // the output, but not the first.
          sb.append(orig.substring(postEsc, thisEsc + 1));
          postEsc = thisEsc + 2;
          break;

        case 'u':
          // Unescape Unicode characters.
          sb.append(orig.substring(postEsc, thisEsc));
          char unicodeChar = 0;
          int ii = thisEsc + 2;
          // The specification permits one or more 'u' characters.
          while (ii < orig.length() && orig.charAt(ii) == 'u') {
            ii++;
          }
          // The specification requires exactly 4 hexadecimal characters.
          // This is more liberal.  (Should it be?)
          int limit = Math.min(ii + 4, orig.length());
          while (ii < limit) {
            int thisDigit = Character.digit(orig.charAt(ii), 16);
            if (thisDigit == -1) {
              break;
            }
            unicodeChar = (char) ((unicodeChar * 16) + thisDigit);
            ii++;
          }
          sb.append(unicodeChar);
          postEsc = ii;
          break;

        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
          // Unescape octal characters.
          sb.append(orig.substring(postEsc, thisEsc));
          char octalChar = 0;
          int iii = thisEsc + 1;
          while (iii < Math.min(thisEsc + 4, orig.length())) {
            int thisDigit = Character.digit(orig.charAt(iii), 8);
            if (thisDigit == -1) {
              break;
            }
            int newValue = (octalChar * 8) + thisDigit;
            if (newValue > 0377) {
              break;
            }
            octalChar = (char) newValue;
            iii++;
          }
          sb.append(octalChar);
          postEsc = iii;
          break;

        default:
          // In the default case, retain the character following the backslash,
          // but discard the backslash itself.  "\*" is just a one-character string.
          sb.append(orig.substring(postEsc, thisEsc));
          postEsc = thisEsc + 1;
          break;
      }
      thisEsc = orig.indexOf('\\', postEsc);
    }
    if (postEsc == 0) {
      return orig;
    }
    sb.append(orig.substring(postEsc));
    return sb.toString();
  }
}
