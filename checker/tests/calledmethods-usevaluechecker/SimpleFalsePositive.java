import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.Filter;

import java.util.*;

// A simple (potential) false positive case with mutliple filters.
public class SimpleFalsePositive {
    void test(AmazonEC2 ec2Client, String namePrefix) {
        DescribeImagesRequest request =
                new DescribeImagesRequest()
                        .withOwners("martin")
                        .withFilters(
                                Arrays.asList(
                                        new Filter("platform", Arrays.asList("windows")),
                                        new Filter(
                                                "name",
                                                Arrays.asList(String.format("%s*", namePrefix)))));
        DescribeImagesResult result = ec2Client.describeImages(request);
    }
}
