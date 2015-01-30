// Test case for Issue 395:
// https://code.google.com/p/checker-framework/issues/detail?id=395

import java.util.*;

@SuppressWarnings({"javari", "oigj"})
class Test {

    Object[] testMethod() {
        return new Object[] { new ArrayList<>() };
    }

}