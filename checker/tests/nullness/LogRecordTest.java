// Test Case for adding nullness annotations to java.util.logging.LogRecord

import java.util.logging.Level;
import java.util.logging.LogRecord;

public class LogRecordTest {

    void test(Level level) {

        LogRecord logRecord = new LogRecord(level, null);

        logRecord.setLoggerName(null);

        logRecord.setResourceBundle(null);

        logRecord.setSourceClassName(null);

        logRecord.setMessage(null);

        logRecord.setSourceMethodName(null);

        logRecord.setParameters(null);

        logRecord.setThrown(null);
    }
}
