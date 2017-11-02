public class Fields {

    // Not final field
    String s = null;

    void setString(String s) {
        this.s = s;
    }

    // Test flow for fields but in different receivers
    void others() {
        Fields withOddField = null;
        Fields notOddField = null;

        withOddField.s = null;
        notOddField.s = "m";
    }
}
