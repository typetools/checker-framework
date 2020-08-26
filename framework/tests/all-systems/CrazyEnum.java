@SuppressWarnings("all") // Check for crashes.
public class CrazyEnum {
    private enum MyEnum {
        ENUM_CONST1 {
            private final String s = method();

            private String method() {
                return "hello";
            }
        },

        ENUM_CONST2 {
            private final String s = this.method();

            private String method() {
                return "hello";
            }
        }
    }
}
