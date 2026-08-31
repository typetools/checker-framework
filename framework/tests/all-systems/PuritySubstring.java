import org.checkerframework.dataflow.qual.SideEffectFree;

public class PuritySubstring {

  @SuppressWarnings({"allcheckers:purity", "lock"}) // local StringBuilder
  @SideEffectFree
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("hello");
    return sb.toString();
  }
}
