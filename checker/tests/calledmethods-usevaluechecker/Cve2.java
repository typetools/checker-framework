import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.Filter;
import java.util.Collections;

// https://nvd.nist.gov/vuln/detail/CVE-2018-15869
public class Cve2 {
  private static final String IMG_NAME = "some_linux_img";

  public static void onlyNames(AmazonEC2 client) {
    // Should not be allowed unless .withOwner is also used
    DescribeImagesRequest request = new DescribeImagesRequest();
    request.withFilters(new Filter("name").withValues(IMG_NAME));

    // :: error: argument
    DescribeImagesResult result = client.describeImages(request);
  }

  public static void correct1(AmazonEC2 client) {
    DescribeImagesRequest request = new DescribeImagesRequest();
    request.withFilters(new Filter("name").withValues(IMG_NAME));
    request.withOwners("martin");

    DescribeImagesResult result = client.describeImages(request);
  }

  public static void correct2(AmazonEC2 client) {
    DescribeImagesRequest request = new DescribeImagesRequest();
    request.withImageIds("myImageId");

    DescribeImagesResult result = client.describeImages(request);
  }

  public static void correct3(AmazonEC2 client) {
    DescribeImagesRequest request = new DescribeImagesRequest();
    request.setExecutableUsers(Collections.singletonList("myUser1"));

    DescribeImagesResult result = client.describeImages(request);
  }
}
