// Examples of trying to prove the key size was set correctly on a AWS GenerateDataKeyRequest object

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.model.DataKeySpec;
import com.amazonaws.services.kms.model.GenerateDataKeyRequest;
import org.checkerframework.checker.calledmethods.qual.*;

public class GenerateDataKeyRequestExamples {

    void correctWithKeySpec(AWSKMS client) {
        GenerateDataKeyRequest request = new GenerateDataKeyRequest();
        request.withKeySpec(DataKeySpec.AES_256);
        client.generateDataKey(request);
    }

    void correctWithNumberOfBytes(AWSKMS client) {
        GenerateDataKeyRequest request = new GenerateDataKeyRequest();
        request.withNumberOfBytes(32);
        client.generateDataKey(request);
    }

    void correctSetKeySpec(AWSKMS client) {
        GenerateDataKeyRequest request = new GenerateDataKeyRequest();
        request.setKeySpec(DataKeySpec.AES_256);
        client.generateDataKey(request);
    }

    void correctSetNumberOfBytes(AWSKMS client) {
        GenerateDataKeyRequest request = new GenerateDataKeyRequest();
        request.setNumberOfBytes(32);
        client.generateDataKey(request);
    }

    // The next four examples are "both"
    void incorrect1(AWSKMS client) {
        GenerateDataKeyRequest request = new GenerateDataKeyRequest();
        request.setKeySpec(DataKeySpec.AES_256);
        request.setNumberOfBytes(32);
        // :: error: argument.type.incompatible
        client.generateDataKey(request);
    }

    void incorrect2(AWSKMS client) {
        GenerateDataKeyRequest request = new GenerateDataKeyRequest();
        request.withKeySpec(DataKeySpec.AES_256);
        request.setNumberOfBytes(32);
        // :: error: argument.type.incompatible
        client.generateDataKey(request);
    }

    void incorrect3(AWSKMS client) {
        GenerateDataKeyRequest request = new GenerateDataKeyRequest();
        request.setKeySpec(DataKeySpec.AES_256);
        request.withNumberOfBytes(32);
        // :: error: argument.type.incompatible
        client.generateDataKey(request);
    }

    void incorrect4(AWSKMS client) {
        GenerateDataKeyRequest request = new GenerateDataKeyRequest();
        request.withKeySpec(DataKeySpec.AES_256);
        request.withNumberOfBytes(32);
        // :: error: argument.type.incompatible
        client.generateDataKey(request);
    }

    // This example is "neither"
    void incorrect5(AWSKMS client) {
        GenerateDataKeyRequest request = new GenerateDataKeyRequest();
        // :: error: argument.type.incompatible
        client.generateDataKey(request);
    }

    // Calling these methods are idempotent, including between with/set versions of the same.
    // TODO: Verify that these calls should be permitted.
    void setTwice1(AWSKMS client) {
        GenerateDataKeyRequest request = new GenerateDataKeyRequest();
        request.withKeySpec(DataKeySpec.AES_256);
        request.withKeySpec(DataKeySpec.AES_256);
        client.generateDataKey(request);
    }

    void setTwice2(AWSKMS client) {
        GenerateDataKeyRequest request = new GenerateDataKeyRequest();
        request.withKeySpec(DataKeySpec.AES_256);
        request.setKeySpec(DataKeySpec.AES_256);
        client.generateDataKey(request);
    }

    void setTwice3(AWSKMS client) {
        GenerateDataKeyRequest request = new GenerateDataKeyRequest();
        request.withNumberOfBytes(32);
        request.setNumberOfBytes(32);
        client.generateDataKey(request);
    }

    void setTwice4(AWSKMS client) {
        GenerateDataKeyRequest request = new GenerateDataKeyRequest();
        request.setNumberOfBytes(32);
        request.setNumberOfBytes(32);
        client.generateDataKey(request);
    }

    /// Interprocedural

    void callee2(
            AWSKMS client,
                    @CalledMethodsPredicate("(!withNumberOfBytes) && (!setNumberOfBytes)") GenerateDataKeyRequest request) {
        request.withKeySpec(DataKeySpec.AES_256);
        client.generateDataKey(request);
    }
}
