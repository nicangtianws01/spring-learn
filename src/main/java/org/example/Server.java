package org.example;

import cn.hutool.core.codec.Base64Decoder;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.EscapeUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.example.component.DemoComponent;
import org.example.controller.DemoController;
import org.example.util.AmountUtil;
import org.example.util.DESEncryptUtil;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Date;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;

@Slf4j
public class Server {

    public static void main(String[] args) {
        ApplicationContext context = new ApplicationContext(new String[]{"org.example.component", "org.example.controller"});
        DemoComponent bean = (DemoComponent) context.getBean("demoComponent");
        DemoController controller = (DemoController) context.getBean("demoController");
        assert bean != null;
        assert controller != null;
        bean.run("");
        controller.request();

        try (ServerSocket server = new ServerSocket()) {
            InetSocketAddress address = new InetSocketAddress("10.130.1.71", 8080);
            server.bind(address);
            while (true) {
                try (Socket socket = server.accept();) {
                    HttpRequest request = new HttpRequest(socket.getInputStream(), socket.getOutputStream());
                    request.run();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class HttpRequest implements Runnable {

        private final InputStream inputStream;
        private final OutputStream outputStream;

        public HttpRequest(InputStream inputStream, OutputStream outputStream) {
            this.inputStream = inputStream;
            this.outputStream = outputStream;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while (!(line = reader.readLine()).isEmpty()) {
                    log.info(line);
                }
                outputStream.write("HTTP/1.1 200\r\n".getBytes(StandardCharsets.UTF_8));
                outputStream.write("Keep-Alive: timeout=60\r\n".getBytes(StandardCharsets.UTF_8));
                outputStream.write("Content-Type: application/json\r\n".getBytes(StandardCharsets.UTF_8));
                outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
                outputStream.write("success".getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            } catch (IOException e) {
                log.error("", e);
            }
        }
    }
}
