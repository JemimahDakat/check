package com.example.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper; //Multipurpose Internet Mail Extensions, can send ,audio, img vid
import org.springframework.stereotype.Service; //in  middle between the controller layer ( incoming ) and the repo (database)
import java.io.UnsupportedEncodingException;


// uses a Simple Mail Transfer Protocol
@Service
public class EmailService {


    // find an existing JavaMailSender bean
    // (configured in application.properties with SMTP credentials)
    // and inject it here — no need to manually instantiate it.
    @Autowired
    private JavaMailSender mailSender;
    //a Spring interface that wraps JavaMail
    //configured in appliaction.properties


    //Builds and sends an HTML verification email to a newly registered user
    public void sendVerificationEmail(String email, String token)
            throws MessagingException, UnsupportedEncodingException {


        String subject = "Verify your Deeblr Account";
        //From name shown in the inbox (not the email address itself)
        String senderName = "Deeblr Team";

        // This is the link the user clicks.
        // by clicking, it sends the JWT token back to backend
        String verifyURL = "http://localhost:8080/api/auth/verify?code=" + token;
        //Hardcoded to localhost — this only works in local development.

        String mailContent = "<p>Dear User,</p>";
        mailContent += "<p>Please click the link below to verify your registration:</p>";
        mailContent += "<h3><a href=\"" + verifyURL + "\">VERIFY ACCOUNT</a></h3>";
        mailContent += "<p>Thank you,<br>The Deeblr Team</p>";

        // produces a raw, empty MimeMessage object.
        //didnt use SimpleMailMessage  is plain-text only
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);

        //  Without a display name, would see email address only
        helper.setFrom("rebeccaaagreen44@gmail.com", senderName);
        helper.setTo(email);
        helper.setSubject(subject);
        helper.setText(mailContent, true); // tells the helper this is HTML

        //Hands the populated MimeMessage off to the mail server via SMTP
        mailSender.send(message);
    }
}