// TEMPORARY

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.Filter;

// https://nvd.nist.gov/vuln/detail/CVE-2018-15869
public class CveSmall2 {
    private static final String IMG_NAME = "some_linux_img";

    // Using impl class instead of interface
    public static void onlyNames(AmazonEC2Client client) {
        // Should not be allowed unless .withOwner is also used
        DescribeImagesResult result =
                client.describeImages(
                        new DescribeImagesRequest()
                                // :: error: argument.type.incompatible
                                .withFilters(new Filter("name").withValues(IMG_NAME)));
    }
}
