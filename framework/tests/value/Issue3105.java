// Test case for issue #3105: https://tinyurl.com/cfissue/3105

// Issue3105Fields is
// framework/src/test/java/org/checkerframework/framework/testchecker/lib/Issue3105Fields.java.
import org.checkerframework.common.value.qual.StringVal;
import org.checkerframework.framework.testchecker.lib.Issue3105Fields;

public class Issue3105 {
  class Demo1 {
    @StringVal("foo") String m() {
      return Issue3105Fields.FIELD1;
    }
  }

  class Demo2 extends Issue3105Fields {
    @StringVal("foo") String m() {
      return FIELD1;
    }
  }

  class Demo3 {
    @StringVal("bar") String m() {
      return Issue3105Fields.FIELD2;
    }
  }

  class Demo4 extends Issue3105Fields {
    @StringVal("bar") String m() {
      return FIELD2;
    }
  }
}
