import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.security.MessageDigest;

public class Encryptor {

    private static final String CIPHER_INSTANCE = "AES/CBC/PKCS5Padding";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: java -jar encrypt.jar <text> <encryption-key>");
            System.exit(1);
        }

        String text = args[0];
        String key = args[1];

        SecretKeySpec secretKey = getKey(key);

        Cipher cipher = Cipher.getInstance(CIPHER_INSTANCE);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] encrypted = cipher.doFinal(text.getBytes("UTF-8"));
        String encoded = Base64.getEncoder().encodeToString(encrypted);

        System.out.println("Encrypted Value = " + encoded);
    }

    private static SecretKeySpec getKey(String myKey) throws Exception {
        byte[] key = myKey.getBytes("UTF-8");

        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        key = sha.digest(key);

        // use first 16 bytes = AES-128
        byte[] keyBytes = new byte[16];
        System.arraycopy(key, 0, keyBytes, 0, 16);

        return new SecretKeySpec(keyBytes, "AES");
    }

    public static String decrypt(String encryptedText, String key) throws Exception {
        SecretKeySpec secretKey = getKey(key);

        Cipher cipher = Cipher.getInstance(CIPHER_INSTANCE);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        byte[] decoded = Base64.getDecoder().decode(encryptedText);
        byte[] decrypted = cipher.doFinal(decoded);

        return new String(decrypted, "UTF-8");
    }
}