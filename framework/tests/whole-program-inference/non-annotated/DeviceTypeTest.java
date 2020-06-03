// In the wild, code like this caused WPI to generate a stub file for DeviceType that used "class"
// instead of "enum".

public class DeviceTypeTest {
    public enum DeviceType {
        TRACKER;
    }

    private final DeviceType deviceType;

    public DeviceTypeTest() {
        deviceType = DeviceType.valueOf("tracker");
    }
}
