package encrypted;

import myqual.Encrypted;

public class EncryptionDemo {
    private final int OFFSET = 13;

    public @Encrypted String encrypt(String text) {
        @Encrypted String encryptedText = new @Encrypted String();
        for (char character : text.toCharArray()) {
            encryptedText += encryptCharacter(character);
        }
        return encryptedText;
    }

    private @Encrypted char encryptCharacter(char character) {
        @Encrypted int encryptInt = (character + OFFSET) % Character.MAX_VALUE;
        return (@Encrypted char) encryptInt;
    }

    // Only send encrypted data!
    public void sendOverInternet(@Encrypted String msg) {
        // ...
    }

    public void sendPassword() {
        String password = getUserPassword();
        sendOverInternet(password);
    }

    private String getUserPassword() {
        return "!@#$Really Good Password**";
    }
}
