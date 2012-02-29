public class UnboxConditions {
  public static void main(String[] args) {
    Boolean b = null;
    //:: error: (condition.nullable)
    if (b) { ; }
    //:: error: (condition.nullable)
    b = b ? b : b;
    //:: error: (condition.nullable)
    while (b) { ; }
    //:: error: (condition.nullable)
    do { ; } while (b);
    //:: error: (condition.nullable)
    for (;b;) { ; }
    // legal!
    for (;;) { break; }
    // Why is eluding the condition in a "for" legal, but not in a "while"?
    // while () {}
  }
}
