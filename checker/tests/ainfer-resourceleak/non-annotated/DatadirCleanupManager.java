import java.io.*;
import java.util.*;

public class DatadirCleanupManager {
  static class PurgeTask extends TimerTask {

    @Override
    public void run() {
      try {
        PurgeTxnLog.purge();
      } catch (Exception e) {

      }
    }
  }
}
