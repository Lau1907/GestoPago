package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final String mailUsername;

    @Autowired
    public EmailServiceImpl(
            @Autowired(required = false) JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String mailUsername) {
        this.mailSender = mailSender;
        this.mailUsername = mailUsername;
    }

    @Override
    public void enviarCodigoOtp(String destinatario, String codigoOtp, int minutosExpiracion) {
        if (mailSender == null || !StringUtils.hasText(mailUsername)) {
            log.warn("Servicio SMTP no configurado (MAIL_USERNAME vacío). No se envió el correo 2FA a {}", destinatario);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailUsername);
            helper.setTo(destinatario);
            helper.setSubject("Tu código de verificación 2FA - GestoPago");

            String htmlBody = """
                    <div style="font-family: Arial, sans-serif; max-width: 520px; margin: 0 auto; padding: 24px; border: 1px solid #e5e7eb; border-radius: 10px; background-color: #ffffff;">
                      <h2 style="color: #111827; margin-top: 0;">Verificación en dos pasos (2FA)</h2>
                      <p style="color: #374151; font-size: 15px; line-height: 1.5;">
                        Utiliza el siguiente código de seguridad de 6 dígitos para completar tu inicio de sesión:
                      </p>
                      <div style="margin: 24px 0; padding: 16px; background-color: #f3f4f6; border-radius: 8px; text-align: center;">
                        <span style="font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #1d4ed8;">%s</span>
                      </div>
                      <p style="color: #4b5563; font-size: 14px; line-height: 1.5;">
                        Este código expirará en <strong>%d minutos</strong>.
                      </p>
                      <p style="color: #6b7280; font-size: 12px; margin-top: 24px;">
                        Si no solicitaste este código, puedes ignorar este mensaje.
                      </p>
                    </div>
                    """.formatted(codigoOtp, minutosExpiracion);

            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Correo con código OTP 2FA enviado exitosamente a {}", destinatario);
        } catch (MessagingException | MailException ex) {
            log.error("Error al enviar correo 2FA a {}: {}", destinatario, ex.getMessage(), ex);
            throw new IllegalStateException("No se pudo enviar el código de verificación por correo electrónico.", ex);
        }
    }
}
