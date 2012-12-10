
public class PrintArray {
    public static final void print(java.io.PrintStream ps, Object[][] a) {
        if (a == null) {
            ps.println("null");
            return;
        }
        // When analyzing this call, we see an exception about taking the LUB
        // of ATMs with different numbers of qualifiers.
        ps.print('7');
    }
}
