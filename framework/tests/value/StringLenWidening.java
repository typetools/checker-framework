// Tests termination of a loop increasing the length of a string

class StringLenWidening {

    // Minimized example from java.util.logging.Logger.entering
    public void entering(Object params[]) {
        String msg = "ENTRY";
        for (int i = 0; i < params.length; i++) {
            msg = msg + i;
        }
    }

    public void repeat(int a) {
        String str = "";
        for (int i = 0; i < a; i++) {
            str += "a";
        }
    }
}
