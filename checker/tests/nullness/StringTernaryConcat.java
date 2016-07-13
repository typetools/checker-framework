public class StringTernaryConcat {

    public String s(Integer start) {
        return start + (start.equals(start) ? "" : "-");
    }
}
