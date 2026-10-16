import java.util.regex.MatchResult;

public class MyMatchResult implements MatchResult {

  @Override
  public int start() {
    group(0);
    group();
    end();
    end(1);
    groupCount();
    start();
    start(19);
    return 0;
  }

  @Override
  public int start(int group) {
    return 0;
  }

  @Override
  public int end() {
    return 0;
  }

  @Override
  public int end(int group) {
    return 0;
  }

  @Override
  public String group() {
    return null;
  }

  @Override
  public String group(int group) {
    return null;
  }

  @Override
  public int groupCount() {
    return 0;
  }
}
