import java.util.List;

public class ConditionalNullCapture {
  <T> void nullArm(boolean b, List<? super List<T>> l) {
    var x = b ? null : l.get(0);
  }

  <T> void noNullArm(boolean b, List<? super List<T>> l) {
    var x = b ? l.get(0) : l.get(0);
  }
}
