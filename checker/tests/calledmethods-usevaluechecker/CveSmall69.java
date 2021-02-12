import com.amazonaws.services.ec2.AmazonEC2AsyncClient;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.Filter;

// https://nvd.nist.gov/vuln/detail/CVE-2018-15869
public class CveSmall69 {
    private static final String IMG_NAME = "some_linux_img";

    // Using async impl class
    public static void onlyNames(AmazonEC2AsyncClient client) {
        // Should not be allowed unless .withOwner is also used
        client.describeImages(
                new DescribeImagesRequest()
                        // :: error: argument.type.incompatible
                        .withFilters(new Filter("name").withValues(IMG_NAME)));
    }
}
