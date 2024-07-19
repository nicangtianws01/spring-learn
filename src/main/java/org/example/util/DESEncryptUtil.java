package org.example.util;

import cn.hutool.core.codec.Base64Decoder;
import cn.hutool.core.codec.Base64Encoder;
import cn.hutool.core.util.HexUtil;

import javax.crypto.*;
import javax.crypto.spec.DESKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;

public class DESEncryptUtil {

    private static final String CIPHER_ALGORITHM = "DES";

    private static final String CIPHER_PADDING = "DES/ECB/PKCS7Padding";

    static {
        // 兼容PKCS7Padding填充模式
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    /**
     * des加密
     *
     * @param src
     * @param key
     * @return
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public static String encrypt(String src, String key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        SecureRandom sr = new SecureRandom();
        DESKeySpec ks = new DESKeySpec(key.getBytes(StandardCharsets.UTF_8));
        SecretKeyFactory skf = SecretKeyFactory.getInstance(CIPHER_ALGORITHM);
        SecretKey sk = skf.generateSecret(ks);
        Cipher cip = Cipher.getInstance(CIPHER_PADDING);
        cip.init(Cipher.ENCRYPT_MODE, sk, sr);
        String dest = Base64Encoder.encode(cip.doFinal(src.getBytes(StandardCharsets.UTF_8)));
        return dest;
    }

    /**
     * des解密
     * @param dest
     * @param key
     * @return
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws NoSuchPaddingException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public static String decrypt(String dest, String key) throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
        byte[] decoded = Base64Decoder.decode(dest);
        SecureRandom sr = new SecureRandom();
        DESKeySpec ks = new DESKeySpec(key.getBytes(StandardCharsets.UTF_8));
        SecretKeyFactory skf = SecretKeyFactory.getInstance(CIPHER_ALGORITHM);
        SecretKey sk = skf.generateSecret(ks);
        Cipher cip = Cipher.getInstance(CIPHER_PADDING);
        cip.init(Cipher.DECRYPT_MODE, sk, sr);
        return new String(cip.doFinal(decoded), StandardCharsets.UTF_8);
    }
}
