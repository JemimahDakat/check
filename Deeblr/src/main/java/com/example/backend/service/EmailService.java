package com.example.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import java.io.UnsupportedEncodingException;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendVerificationEmail(String email, String token)
            throws MessagingException, UnsupportedEncodingException {

        String subject = "Verify your Deeblr Account";
        String senderName = "Deeblr Team";

        // This is the link the user clicks.
        // by clicking, it sends the JWT token back to backend
        String verifyURL = "http://localhost:8080/api/auth/verify?code=" + token;

        String mailContent = "<p>Dear User,</p>";
        mailContent += "<p>Please click the link below to verify your registration:</p>";
        mailContent += "<h3><a href=\"" + verifyURL + "\">VERIFY ACCOUNT</a></h3>";
        mailContent += "<p>Thank you,<br>The Deeblr Team</p>";

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        helper.setFrom("rebeccaaagreen44@gmail.com", senderName);
        helper.setTo(email);
        helper.setSubject(subject);
        helper.setText(mailContent, true); // true enables HTML FORMATTING

        mailSender.send(message);
    }
}