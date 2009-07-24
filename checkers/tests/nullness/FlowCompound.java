import checkers.nullness.quals.*;
import java.util.*;

class FlowCompound {

    public boolean equals(Object o) {
        return o != null && this.getClass() != o.getClass();
    }

    void test() {

        String s = "foo";

        if (s == null || s.length() > 0) {
            @NonNull String test = s;
        }

        String tmp;
        @NonNull String notNull;
        tmp = "hello";
        notNull = tmp;
        notNull = tmp = "hello";

    }

  public static boolean equal(@Nullable Object a, @Nullable Object b) {
      assert b != null;
    return a == b || (a != null && a.equals(b));
  }


}
