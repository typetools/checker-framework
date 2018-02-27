// Test case for interaction between ClassVal and getPackage()

// @skip-test

public class GetPackage {

    void callGetPackage() {

        @NonNull Package p1 = GetPackage.class.getPackage();
        @NonNull Package p2 = java.util.List.class.getPackage();
    }
}
