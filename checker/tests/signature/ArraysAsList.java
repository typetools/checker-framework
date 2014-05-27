import org.checkerframework.checker.signature.qual.*;

import java.util.*;

public class ArraysAsList {

  List<String> m() {
    return Arrays.asList("id", "department_id", "permission_id",
                         "expected_connection_time");
  }

}
