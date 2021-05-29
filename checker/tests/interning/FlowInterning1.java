/*
 * A test inspired by some problem in Daikon.
 */
public class FlowInterning1 {

  void test(String[] tokens, int i) {
    String arg_type_name = tokens[i].intern();
    if (i + 1 >= tokens.length) {
      throw new RuntimeException("No matching arg val for argument type " + arg_type_name);
    }
    String arg_val = tokens[i + 1];
    if (arg_type_name == "boolean") { // interned
      // ...
    }
  }
}
