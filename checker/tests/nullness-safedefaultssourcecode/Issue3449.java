// Test case for https://tinyurl.com/cfissue/3449

import org.checkerframework.framework.qual.AnnotatedFor;

@AnnotatedFor("nullness")
public class Issue3449 {

  int length;
  Object[] objs;

  public Issue3449(Object... args) {
    length = args.length;
    objs = args;
  }
}
