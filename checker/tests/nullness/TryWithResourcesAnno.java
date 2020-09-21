// Test case for https://tinyurl.com/cfissue/3305 .

import org.checkerframework.checker.nullness.qual.Nullable;

public class TryWithResourcesAnno {
    public static void f() {
        try (@Nullable AutoCloseable obj = null) {
        } catch (Exception e) {
        }
    }

    public static void g() {
        try (@Nullable AutoCloseable obj1 = null;
                AutoCloseable obj2 = null) {
        } catch (Exception e) {
        }
    }

    public static void h() {
        try (AutoCloseable obj1 = null;
                @Nullable AutoCloseable obj2 = null) {
        } catch (Exception e) {
        }
    }
}
