@checkers.quals.DefaultQualifier("Nullable") public class MyException extends Exception {

     public MyException() { }

     public final String getTotalTrace() {
         final StringBuilder sb = new StringBuilder();
         for(StackTraceElement st: getStackTrace()) {
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
