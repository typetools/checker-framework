// Based on a false positive in hdfs
// A test that shows we are handling the sequence of method invocations correctly

import java.io.IOException;
import java.nio.channels.FileLock;

class GetChannelOnLocks {
    public boolean isLockSupported(FileLock lock, FileLock lock1, FileLock lock2)
            throws IOException {
        FileLock firstLock = null;
        FileLock secondLock = null;
        try {
            firstLock = lock1;
            if (firstLock == null) {
                return true;
            }
            secondLock = lock2;
            if (secondLock == null) {
                return true;
            }
        } finally {
            if (firstLock != null && firstLock != lock) {
                firstLock.release();
                firstLock.channel().close();
            }
            if (secondLock != null) {
                secondLock.release();
                secondLock.channel().close();
            }
        }
        return false;
    }

    public void isLockSupported2(FileLock lock, FileLock firstLock) throws IOException {
        if (firstLock != null && firstLock != lock) {
            firstLock.release();
            firstLock.channel().close();
        }
    }
}
