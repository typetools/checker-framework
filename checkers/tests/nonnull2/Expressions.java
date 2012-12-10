import checkers.nullness.quals.*;
import checkers.quals.DefaultQualifier;
import java.util.HashMap;
import java.util.regex.*;
import java.io.*;
import java.util.*;

public class Expressions {

  void test4(List<? extends @NonNull Object> o) {
      o.get(0).getClass();  // valid
  }

}
