import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.Filter;

import java.util.*;

// A test to ensure that requests that are created in the call to describeImages work correctly.
public class RequestCreatedInCall {
    void test(AmazonEC2 ec2) {
        List<Filter> filters = new ArrayList<>();
        filters.add(new Filter().withName("foo").withValues("bar"));
        DescribeImagesResult describeImagesResult =
                ec2.describeImages(
                        new DescribeImagesRequest().withOwners("martin").withFilters(filters));
    }
}
