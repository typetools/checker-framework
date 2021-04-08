import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.Filter;
import java.util.Arrays;
import java.util.Collections;

public class MorePreciseFilters {

  /* TODO: handle lists
  void ownerAliasList(AmazonEC2 ec2Client) {
      DescribeImagesRequest imagesRequest = new DescribeImagesRequest();
      List<Filter> imageFilters = new ArrayList<Filter>();
      imageFilters.add(new Filter().withName("owner-alias").withValues("microsoft"));
      ec2Client.describeImages(imagesRequest.withFilters(imageFilters)).getImages();
  }
  */

  void withFilterNameInList(AmazonEC2 ec2Client) {
    DescribeImagesRequest request = new DescribeImagesRequest();
    request.setFilters(
        Collections.singletonList(new Filter().withName("image-id").withValues("12345")));

    DescribeImagesResult result = ec2Client.describeImages(request);
  }

  void withOwnerId(AmazonEC2 ec2) {
    DescribeImagesRequest request =
        new DescribeImagesRequest()
            .withFilters(
                new Filter("name", Arrays.asList("my_image_name")),
                new Filter("owner-id", Arrays.asList("12345")));
    DescribeImagesResult result = ec2.describeImages(request);
  }

  void withName(AmazonEC2 ec2Client) {
    DescribeImagesRequest request = new DescribeImagesRequest();
    request.withFilters(new Filter().withName("image-id").withValues("12345"));
    DescribeImagesResult result = ec2Client.describeImages(request);
  }

  void withName2(AmazonEC2 ec2Client) {
    DescribeImagesRequest request = new DescribeImagesRequest();
    request.withFilters(new Filter().withName("image-id").withName("foo").withValues("12345"));
    // :: error: (argument.type.incompatible)
    DescribeImagesResult result = ec2Client.describeImages(request);
  }

  void withName3(AmazonEC2 ec2Client) {
    DescribeImagesRequest request = new DescribeImagesRequest();
    request.withFilters(new Filter().withName("foo").withName("image-id").withValues("12345"));
    DescribeImagesResult result = ec2Client.describeImages(request);
  }

  void withName4(AmazonEC2 ec2Client) {
    DescribeImagesRequest request = new DescribeImagesRequest();
    request.withFilters(
        new Filter().withName("owner-id").withName("foo").withValues("12345"),
        new Filter("owner-id", Arrays.asList("12345")));
    DescribeImagesResult result = ec2Client.describeImages(request);
  }

  void withName5(AmazonEC2 ec2Client) {
    DescribeImagesRequest request = new DescribeImagesRequest();
    Filter f = new Filter();
    request.withFilters(f.withName("owner-id").withValues("12345"));
    DescribeImagesResult result = ec2Client.describeImages(request);
  }
}
