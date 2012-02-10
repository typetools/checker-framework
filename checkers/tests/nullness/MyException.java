@checkers.quals.DefaultQualifier("Nullable") public class MyException extends Exception {

     public MyException() { }

     public final String getTotalTrace() {
         final StringBuilder sb = new StringBuilder();
         //:: error: (dereference.of.nullable)
         for(StackTraceElement st: getStackTrace()) {
           //:: error: (dereference.of.nullable)
            sb.append(st.toString());
            sb.append("\n");
         }
         return sb.toString();
     }

     @SuppressWarnings("nullness")
     public StackTraceElement[] getStackTrace() {
         throw new RuntimeException("not implemented yet");
     }
}
