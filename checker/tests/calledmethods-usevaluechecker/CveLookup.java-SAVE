// TEMPORARY

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;

// https://nvd.nist.gov/vuln/detail/CVE-2018-15869
public class CveLookup {
    public static void onlyNames(AmazonEC2 client) {
        // Should not be allowed unless .withOwner is also used
        client.describeImages(null);
    }

    // Using impl class instead of interface
    public static void onlyNames(AmazonEC2Client client) {
        // Should not be allowed unless .withOwner is also used
        client.describeImages(null);
    }
}
