public class CompoundAssign {
  void m(String args) {
    String arg = "";
    for (int ii = 0; ii < args.length(); ii++) {
      if ('x' == 'y') {
        arg += 'x';
      } else {
        arg = "";
      }
    }
    if (arg.equals("")) {}
  }
}
