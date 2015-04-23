import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

public interface Interface {

    @EnsuresNonNull("f")
    public void setf();

    public void setg();

}
