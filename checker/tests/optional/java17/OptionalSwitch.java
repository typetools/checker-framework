// @below-java17-jdk-skip-test
public class OptionalSwitch {
  public static boolean flag;

  public Object test(int c) {
    return switch (c) {
      case 3 -> flag ? "" : "obj.getBaseStat();";
      default -> null;
    };
  }
}
