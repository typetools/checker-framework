// @skip-test crash, but disabled to avoid breaking the build

public class StringTernaryConcat {

    public String s(Integer start) {
        return start + (start.equals(start) ? "" : "-");
    }

}
