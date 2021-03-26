import org.checkerframework.common.value.qual.IntVal;

public class ClassInitializer {

  @IntVal(1) int x;

  @IntVal(2) int y;

  int z;

  {
    y = 2;
  }

  ClassInitializer() {
    x = 1;
  }

  ClassInitializer(boolean ignore) {
    x = 1;
    z = 3;
  }
}
