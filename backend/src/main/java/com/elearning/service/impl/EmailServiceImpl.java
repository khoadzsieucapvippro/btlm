package com.elearning.service.impl;

import com.elearning.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Robust Email Delivery Service using Java 21 HttpClient.
 * Dispatches real transactional emails via Resend API or Brevo API over HTTPS (port 443).
 * When external credentials are not set, fails fast and gracefully returns false
 * so that the caller can provide local dev fallback.
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${app.email.resend-api-key:${RESEND_API_KEY:}}")
    private String resendApiKey;

    @Value("${app.email.brevo-api-key:${BREVO_API_KEY:}}")
    private String brevoApiKey;

    @Value("${app.email.brevo-sender-email:${BREVO_SENDER_EMAIL:noreply@elearning-chinese.com}}")
    private String brevoSenderEmail;

    @Value("${app.email.from-email:${APP_EMAIL_FROM:onboarding@resend.dev}}")
    private String fromEmail;

    @Value("${app.email.from-name:${APP_EMAIL_FROM_NAME:Hán Ngữ Học Đường}}")
    private String fromName;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:${SPRING_MAIL_USERNAME:}}")
    private String mailUsername;

    public EmailServiceImpl(ObjectMapper objectMapper) {
        this(objectMapper, null);
    }

    @Autowired
    public EmailServiceImpl(ObjectMapper objectMapper, @Autowired(required = false) JavaMailSender mailSender) {
        this.objectMapper = objectMapper;
        this.mailSender = mailSender;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public boolean sendPasswordResetCode(String toEmail, String resetCode) {
        if (toEmail == null || toEmail.trim().isEmpty() || resetCode == null || resetCode.trim().isEmpty()) {
            log.warn("Bỏ qua gửi email: toEmail hoặc resetCode bị trống.");
            return false;
        }

        String recipient = toEmail.trim().toLowerCase();
        log.info("Bắt đầu xử lý gửi email xác thực mã quên mật khẩu tới: {}", recipient);

        // 1. Try Brevo API over HTTPS (port 443) - sends to ANY Gmail/email address worldwide
        if (brevoApiKey != null && !brevoApiKey.trim().isEmpty()) {
            boolean success = sendViaBrevo(recipient, resetCode);
            if (success) {
                return true;
            }
        }

        // 2. Try Resend API over HTTPS (port 443)
        if (resendApiKey != null && !resendApiKey.trim().isEmpty()) {
            boolean success = sendViaResend(recipient, resetCode);
            if (success) {
                return true;
            }
        }

        // 3. Try Gmail / Standard SMTP via JavaMailSender if configured (Local dev)
        if (mailSender != null && mailUsername != null && !mailUsername.trim().isEmpty()) {
            boolean success = sendViaSmtp(recipient, resetCode);
            if (success) {
                return true;
            }
        }

        // 3. Fallback: FormSubmit Webhook relay
        boolean webhookSent = sendViaFormSubmit(recipient, resetCode);
        if (webhookSent) {
            return true;
        }

        log.info("Không có cổng gửi email nào hoạt động hoặc dịch vụ bên ngoài chưa kích hoạt. Mã OTP: {}", resetCode);
        return false;
    }

    private boolean sendViaResend(String recipient, String resetCode) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("from", fromName + " <" + fromEmail + ">");
            payload.put("to", List.of(recipient));
            payload.put("subject", "Mã xác nhận đặt lại mật khẩu - Hán Ngữ Học Đường");
            payload.put("html", buildHtmlContent(recipient, resetCode));

            String jsonBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + resendApiKey.trim())
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Elearning/1.0.0")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Gửi email thành công qua Resend API tới {}. Response: {}", recipient, response.body());
                return true;
            } else {
                log.warn("Resend API trả về mã lỗi: {} - Body: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Lỗi khi gửi email qua Resend API tới {}: {}", recipient, e.getMessage());
        }
        return false;
    }

    private boolean sendViaBrevo(String recipient, String resetCode) {
        try {
            Map<String, Object> payload = new HashMap<>();
            Map<String, String> sender = new HashMap<>();
            sender.put("name", fromName);
            String senderEmail = (brevoSenderEmail != null && !brevoSenderEmail.isBlank())
                    ? brevoSenderEmail.trim()
                    : "noreply@elearning-chinese.com";
            sender.put("email", senderEmail);
            payload.put("sender", sender);

            Map<String, String> toRecipient = new HashMap<>();
            toRecipient.put("email", recipient);
            payload.put("to", List.of(toRecipient));

            payload.put("subject", "Mã xác nhận đặt lại mật khẩu - Hán Ngữ Học Đường");
            payload.put("htmlContent", buildHtmlContent(recipient, resetCode));

            String jsonBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .timeout(Duration.ofSeconds(10))
                    .header("api-key", brevoApiKey.trim())
                    .header("accept", "application/json")
                    .header("content-type", "application/json")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Elearning/1.0.0")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Gửi email thành công qua Brevo API tới {}. Response: {}", recipient, response.body());
                return true;
            } else {
                log.warn("Brevo API trả về mã lỗi: {} - Body: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Lỗi khi gửi email qua Brevo API tới {}: {}", recipient, e.getMessage());
        }
        return false;
    }

    private boolean sendViaFormSubmit(String recipient, String resetCode) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("_subject", "Mã xác thực đặt lại mật khẩu - Hán Ngữ Học Đường");
            payload.put("Mã xác nhận 6 chữ số", resetCode);
            payload.put("Thời hạn hiệu lực", "15 phút");
            payload.put("Lưu ý", "Tuyệt đối không chia sẻ mã này cho bất kỳ ai");
            payload.put("Hệ thống", "Hán Ngữ Học Đường");
            payload.put("_captcha", "false");
            payload.put("_template", "box");

            String jsonBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://formsubmit.co/ajax/" + recipient))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Referer", "https://khoadzsieucapvippro.github.io/WebLearnChinese/login.html")
                    .header("Origin", "https://khoadzsieucapvippro.github.io")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300 && response.body().contains("\"success\":\"true\"")) {
                log.info("Gửi email thành công qua FormSubmit relay tới {}. Response: {}", recipient, response.body());
                return true;
            } else {
                log.warn("FormSubmit relay phản hồi: {}", response.body());
            }
        } catch (Exception e) {
            log.error("FormSubmit relay không thể gửi tới {}: {}", recipient, e.getMessage());
        }
        return false;
    }

    private boolean sendViaSmtp(String recipient, String resetCode) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            String senderAddress = (mailUsername != null && !mailUsername.isBlank()) ? mailUsername.trim() : fromEmail;
            helper.setFrom(new InternetAddress(senderAddress, fromName, "UTF-8"));
            helper.setTo(recipient);
            helper.setSubject("Mã xác thực đặt lại mật khẩu - Hán Ngữ Học Đường");
            helper.setText(buildHtmlContent(recipient, resetCode), true);

            mailSender.send(mimeMessage);
            log.info("Gửi email thành công qua Gmail SMTP tới {}", recipient);
            return true;
        } catch (Exception e) {
            log.warn("Gửi email qua Gmail SMTP thất bại tới {}: {}", recipient, e.getMessage());
            return false;
        }
    }

    private String buildHtmlContent(String recipient, String resetCode) {
        return "<div style=\"font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;\">" +
                "<h2 style=\"color: #8B2635; margin-top: 0;\">Hán Ngữ Học Đường</h2>" +
                "<p>Xin chào,</p>" +
                "<p>Bạn vừa yêu cầu đặt lại mật khẩu cho tài khoản <strong>" + recipient + "</strong> trên hệ thống học tiếng Trung Hán Ngữ Học Đường.</p>" +
                "<div style=\"background-color: #f8f9fa; padding: 15px; text-align: center; border-radius: 6px; margin: 20px 0;\">" +
                "<p style=\"margin: 0; font-size: 14px; color: #6c757d;\">Mã xác nhận của bạn là:</p>" +
                "<h1 style=\"letter-spacing: 6px; color: #8B2635; margin: 10px 0; font-size: 32px;\">" + resetCode + "</h1>" +
                "<p style=\"margin: 0; font-size: 13px; color: #dc3545;\">Mã có hiệu lực trong vòng 15 phút. Tuyệt đối không chia sẻ mã này cho người khác.</p>" +
                "</div>" +
                "<p>Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này. Mật khẩu hiện tại của bạn vẫn được bảo mật an toàn.</p>" +
                "<hr style=\"border: none; border-top: 1px solid #eee; margin: 20px 0;\">" +
                "<p style=\"font-size: 12px; color: #999; margin: 0;\">© 2026 Hán Ngữ Học Đường. Nền tảng học Hán tự và bộ thủ trực tuyến chuẩn mực.</p>" +
                "</div>";
    }
}
