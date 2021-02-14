import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.Filter;

public class WithOwnersFilter {
    private static final String IMG_NAME = "some_linux_img";

    public static void correct1(AmazonEC2 client) {
        DescribeImagesResult result =
                client.describeImages(
                        new DescribeImagesRequest()
                                .withFilters(new Filter("name").withValues(IMG_NAME))
                                .withFilters(new Filter("owner").withValues("my_aws_acct")));
    }

    public static void correct2(AmazonEC2 client) {
        DescribeImagesRequest request = new DescribeImagesRequest();
        request.withFilters(new Filter("name").withValues(IMG_NAME));
        request.withFilters(new Filter("owner").withValues("my_aws_acct"));
        client.describeImages(request);
    }

    public static void correct3(AmazonEC2 client) {
        DescribeImagesRequest request = new DescribeImagesRequest();
        request.withFilters(
                new Filter("name").withValues(IMG_NAME),
                new Filter("owner").withValues("my_aws_acct"));
        client.describeImages(request);
    }
}
