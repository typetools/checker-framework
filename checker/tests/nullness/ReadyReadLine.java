
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;


class ReadyReadLine {

  void m(BufferedReader buf) throws Exception {
    if (buf.ready()) {
      String line = buf.readLine();
      line.toString();
    }

    if (buf.readLine() != null) {
        //:: error: (dereference.of.nullable)
        buf.readLine().toString();
    }
  }

}

// this is a replication of the JDK BufferedReader (with only the relevant methods)
class BufferedReader {
    public @Nullable
    String readLine() throws Exception {
        return null;
    }

    @EnsuresNonNullIf(expression = "readLine()", result = true)
    @Pure
    public boolean ready() throws Exception {
        // don't bother with implementation.
        //:: error: (contracts.conditional.postcondition.not.satisfied)
        return true;
    }
}
