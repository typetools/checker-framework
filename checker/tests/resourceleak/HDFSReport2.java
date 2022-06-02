import java.io.Closeable;

class RamDiskAsyncLazyPersistService {

    public interface FsVolumeReference extends Closeable {}

    class ReplicaLazyPersistTask implements Runnable {
        private final FsVolumeReference targetVolume;

        ReplicaLazyPersistTask(FsVolumeReference targetVolume) {
            this.targetVolume = targetVolume;
        }

        @Override
        public void run() {
            try (FsVolumeReference ref = this.targetVolume) {

            } catch (Exception e) {
            }
        }
    }
}
