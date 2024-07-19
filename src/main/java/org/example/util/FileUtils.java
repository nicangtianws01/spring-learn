package org.example.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public final class FileUtils {

    private FileUtils() {
    }

    public static boolean isClazzFile(File file) {
        try (InputStream in = Files.newInputStream(file.toPath());) {
            byte[] bytes = new byte[20];
            in.read(bytes, 0, 20);
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append(Integer.toHexString(b & 0xFF));
            }
            String type = builder.toString();
            return StrUtils.startWithIgnoreCase(type, "CAFEBABE");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
