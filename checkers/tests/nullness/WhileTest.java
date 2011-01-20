import checkers.nullness.quals.Nullable;
import checkers.nullness.quals.NonNull;

public class WhileTest {
	public static void main(String[] args) {
		@Nullable Integer z;
        @NonNull Integer nnz = 3;

        z = null;
        //:: (assignment.type.incompatible)
        nnz = z;

        while (z == null) {
        	break;
        }
        //:: (assignment.type.incompatible)
        nnz = z;
        nnz.toString();
	}
}