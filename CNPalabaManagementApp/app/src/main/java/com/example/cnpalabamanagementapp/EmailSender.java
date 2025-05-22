package com.example.cnpalabamanagementapp;

import android.util.Log;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailSender {

    // Configure these (see Step 3 for Gmail setup)
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String SENDER_EMAIL = "heji042918@gmail.com";
    private static final String SENDER_PASSWORD = "cyzk nuxb apjj rzwj"; // App Password

    public static void sendEmail(String recipientEmail, String subject, String body) {
        Log.d("EmailDebug", "Attempting to send to: " + recipientEmail);
        new Thread(() -> {
            try {
                // 1. Configure SMTP properties
                Properties props = new Properties();
                props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
                props.put("mail.smtp.ssl.protocols", "TLSv1.2");
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", SMTP_HOST);
                props.put("mail.smtp.port", SMTP_PORT);


                // 2. Create authentication session
                Session session = Session.getInstance(props,
                        new Authenticator() {
                            @Override
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                            }
                        });

                // 3. Build the email message
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SENDER_EMAIL));
                message.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse(recipientEmail));
                message.setSubject(subject);
                message.setContent(body, "text/html");

                // 4. Send the email
                Transport.send(message);
                Log.d("EmailDebug", "Email sent successfully to: " + recipientEmail);
            } catch (Exception e) {
                Log.e("EmailDebug", "Failed to send to " + recipientEmail, e);
                e.printStackTrace();
            }
        }).start(); // Run in background thread
    }
}