public class SAMLineParser {

  private int x;

  private String makeErrorString() {
    return "" + (this.x <= 0 ? "" : this.x);
  }
}
