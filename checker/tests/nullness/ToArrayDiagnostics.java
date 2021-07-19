import java.util.ArrayList;

public class ToArrayDiagnostics {

    String[] ok2(ArrayList<String> list) {
        return list.toArray(new String[] {});
    }

    String[] ok3(ArrayList<String> list) {
        return list.toArray(new String[0]);
    }

    String[] ok4(ArrayList<String> list) {
        return list.toArray(new String[list.size()]);
    }

    String[] warn1(ArrayList<String> list) {
        // :: error: (new.array.type.invalid)
        String[] resultArray = new String[list.size()];
        // :: error: (return.type.incompatible) :: warning: (toarray.nullable.elements.not.newarray)
        return list.toArray(resultArray);
    }

    String[] warn2(ArrayList<String> list) {
        int size = list.size();
        // :: error: (new.array.type.invalid) :: error: (return.type.incompatible) :: warning:
        // (toarray.nullable.elements.mismatched.size)
        return list.toArray(new String[size]);
    }
}
