import java.util.logging.LogRecord;
import java.util.logging.Level;

public class LogRecordTest {

    @SuppressWarnings("initialization.fields.uninitialized") // Consturctor for java.util.logging.Level is protected
    Level level;

    void test() {
        
        // :: error: (argument.type.incompatible)
        LogRecord logRecord=new LogRecord(level,null);
        logRecord.setLoggerName(null);
        logRecord.setResourceBundle(null);
        logRecord.setSourceClassName(null);
        logRecord.setMessage(null);
        logRecord.setSourceMethodName(null);
        logRecord.setParameters(null);
        logRecord.setThrown(null);
    }
}