public class SwitchTest {

    public String findSlice_unordered(String[] vis) {
        switch (vis.length) {
            case 1:
                return vis[0];
            case 2:
                return vis[0] + vis[1];
            case 3:
                return vis[0] + vis[1] + vis[2];
            default:
                throw new RuntimeException("Bad length " + vis.length);
        }
    }
}
