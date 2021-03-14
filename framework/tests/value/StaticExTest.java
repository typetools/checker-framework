import org.checkerframework.common.value.qual.*;

public class StaticExTest {
  boolean flag;

  void test1() {
    String s = "helloworlod";
    @StringVal({"o", "l"}) String subString = flag ? "o" : "l";
    @IntVal({5, 0, 9}) int start = flag ? 9 : flag ? 5 : 0;
    // flag?1:flag?6:
    @IntVal({-1, 8, 9, 2, 4, 6}) int result = s.indexOf(subString, start);
  }

  void test2() {
    String s = flag ? "helloworlod" : "lololxxolxxxol";
    @StringVal({"o", "l"}) String subString = flag ? "o" : "l";
    @IntVal({0, 9}) int start = flag ? 9 : 0;
    // flag?1:flag?6:
    @IntVal({-1, 0, 1, 2, 4, 9, 12, 13}) int result3 = s.indexOf(subString, start);
  }

  void test3() {
    @IntVal({0, 1}) int offset = flag ? 0 : 1;
    char[] data = {'h', 'e', 'l', 'l', 'o', 'b', 'y', 'e', 't', 'o'};
    @IntVal({5, 6}) int charCount = flag ? 5 : 6;
    @StringVal({"hello", "ellob", "hellob", "elloby"}) String s = new String(data, offset, charCount);
  }

  void test4() {
    @IntVal({0, 1}) int offset = flag ? 0 : 1;
    char[] data1 = {'h', 'e', 'l', 'l', 'o', 'b', 'y', 'e', 't', 'o'};
    char[] data2 = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j'};
    char @StringVal({"hellobyeto", "abcdefghij"}) [] data = flag ? data1 : data2;
    @IntVal({5, 6}) int charCount = flag ? 5 : 6;
    @StringVal({"hello", "ellob", "hellob", "elloby", "abcde", "bcdef", "abcdef", "bcdefg"}) String s = new String(data, offset, charCount);
  }

  static byte[] b = new byte[0];

  void constructorsArrays() {
    char @ArrayLen(100) [] c = new char[100];
    String s = new String(c);
    new String(b);
  }
}
