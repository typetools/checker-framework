import org.checkerframework.checker.minlen.qual.*;

class LengthTransfer {
    void exceptional_control_flow(int[] a) {
	if (a.length == 0) {
	    throw new IllegalArgumentException();
	}
	int @MinLen(1) [] b = a;
    }

    void equal_to_return(int[] a) {
	if (a.length == 0) {
	    return;
	}
	int @MinLen(1) [] b = a;
    }

    void gt_check(int[] a) {
	if (a.length > 0) {
	    int @MinLen(1) [] b = a;
	}
    }
}
