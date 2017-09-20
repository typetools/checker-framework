public class SwitchDataflowRefinement {

    void readInfo(String[] parts) {

        if (parts.length >= 1) {
            Integer.parseInt(parts[0]);
        }

        switch (parts.length) {
            case 1:
                Integer.parseInt(parts[0]);
                break;
        }

        switch (parts.length) {
            case 0:
                break;
            default:
                Integer.parseInt(parts[0]);
                break;
        }
    }
}
