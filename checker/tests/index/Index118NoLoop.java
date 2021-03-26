import org.checkerframework.common.value.qual.ArrayLen;

public class Index118NoLoop {

  public static void foo(String @ArrayLen(4) [] args, int i) {
    if (i >= 1 && i <= 3) {
      System.out.println(args[i]);
    }
  }
}
