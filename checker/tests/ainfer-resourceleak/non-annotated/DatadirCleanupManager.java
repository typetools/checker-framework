import java.io.*;
import java.util.*;

public class DatadirCleanupManager {

  void start(File dataDir, File snapDir, int count) {
    TimerTask task = new PurgeTask(dataDir, snapDir, count);
  }

  static class PurgeTask extends TimerTask {

    private File logsDir;
    private File snapsDir;
    private int snapRetainCount;

    public PurgeTask(File dataDir, File snapDir, int count) {
      logsDir = dataDir;
      snapsDir = snapDir;
      snapRetainCount = count;
    }

    @Override
    public void run() {
      try {
        PurgeTxnLog.purge(logsDir, snapsDir, snapRetainCount);
      } catch (Exception e) {

      }
    }
  }
}
