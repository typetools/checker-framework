import java.util.*;
import org.checkerframework.checker.signature.qual.*;

public class ArraysAsList {

    List<String> m() {
        return Arrays.asList("id", "department_id", "permission_id", "expected_connection_time");
    }
}
