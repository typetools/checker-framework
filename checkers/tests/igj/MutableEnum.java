import java.util.Date;



public enum MutableEnum {
    PRIVATE;

    public int f = 1;
    public Date d = null;

    // self mutating method
    public void mutate() {
        // reassign fields
        f++;
        d = new Date();

        // mutate field
        d.setTime(1);
    }


    // other mutate method
    public static void mutateEnum() {
        // reassign fields
        PRIVATE.f++;
        PRIVATE.d = new Date();

        // mutate field
        PRIVATE.d.setTime(1);
    }
}
