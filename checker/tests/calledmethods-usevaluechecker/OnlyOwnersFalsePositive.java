import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import java.util.Collections;

// Tests that just setting with/setOwners is permitted, since there are legitimate reasons to do
// that.
// Originally, we required with/setFilters && with/setOwners.
public class OnlyOwnersFalsePositive {
  void test(AmazonEC2 ec2Client) {
    DescribeImagesRequest describeImagesRequest = new DescribeImagesRequest();
    describeImagesRequest.setOwners(Collections.singleton("self"));
    DescribeImagesResult describeImagesResult = ec2Client.describeImages(describeImagesRequest);
  }
}
