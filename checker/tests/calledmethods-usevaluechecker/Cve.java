import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2AsyncClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.Filter;

// https://nvd.nist.gov/vuln/detail/CVE-2018-15869
public class Cve {
    private static final String IMG_NAME = "some_linux_img";

    public static void onlyNames(AmazonEC2 client) {
        // Should not be allowed unless .withOwner is also used
        DescribeImagesResult result =
                client.describeImages(
                        new DescribeImagesRequest()
                                // :: error: argument.type.incompatible
                                .withFilters(new Filter("name").withValues(IMG_NAME)));
    }

    public static void correct1(AmazonEC2 client) {
        DescribeImagesResult result =
                client.describeImages(
                        new DescribeImagesRequest()
                                .withFilters(new Filter("name").withValues(IMG_NAME))
                                .withOwners("martin"));
    }

    public static void correct2(AmazonEC2 client) {
        DescribeImagesResult result =
                client.describeImages(new DescribeImagesRequest().withImageIds("myImageId"));
    }

    public static void correct3(AmazonEC2 client) {
        DescribeImagesResult result =
                client.describeImages(new DescribeImagesRequest().withExecutableUsers("myUsers"));
    }

    // Using impl class instead of interface
    public static void onlyNames(AmazonEC2Client client) {
        // Should not be allowed unless .withOwner is also used
        DescribeImagesResult result =
                client.describeImages(
                        new DescribeImagesRequest()
                                // :: error: argument.type.incompatible
                                .withFilters(new Filter("name").withValues(IMG_NAME)));
    }

    public static void correct1(AmazonEC2Client client) {
        DescribeImagesResult result =
                client.describeImages(
                        new DescribeImagesRequest()
                                .withFilters(new Filter("name").withValues(IMG_NAME))
                                .withOwners("martin"));
    }

    public static void correct2(AmazonEC2Client client) {
        DescribeImagesResult result =
                client.describeImages(new DescribeImagesRequest().withImageIds("myImageId"));
    }

    // Using async impl class
    public static void onlyNames(AmazonEC2AsyncClient client) {
        // Should not be allowed unless .withOwner is also used
        DescribeImagesResult result =
                client.describeImages(
                        new DescribeImagesRequest()
                                // :: error: argument.type.incompatible
                                .withFilters(new Filter("name").withValues(IMG_NAME)));
    }

    public static void correct1(AmazonEC2AsyncClient client) {
        DescribeImagesResult result =
                client.describeImages(
                        new DescribeImagesRequest()
                                .withFilters(new Filter("name").withValues(IMG_NAME))
                                .withOwners("martin"));
    }

    public static void correct2(AmazonEC2AsyncClient client) {
        DescribeImagesResult result =
                client.describeImages(new DescribeImagesRequest().withImageIds("myImageId"));
    }
}
