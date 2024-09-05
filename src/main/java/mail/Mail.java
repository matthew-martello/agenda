package mail;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Properties;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.MessagingException;
import javax.mail.Session;

import org.apache.commons.codec.binary.Base64;

public class Mail {
  private static final String APPLICATION_NAME = "Agenda - SendMessage";
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  
  /**
   * Send an email from the user's mailbox to its recipient.
   *
   * @param fromEmailAddress - Email address to appear in the from: header
   * @param toEmailAddress   - Email address of the recipient
   * @param messageSubject   - Subject of the message.
   * @param bodyText         - Html to be sent in email.
   * @return the sent message, {@code null} otherwise.
   * @throws MessagingException - if a wrongly formatted address is encountered.
   * @throws IOException        - if service account credentials file not found.
   */
  public static Message sendEmail(String fromEmailAddress,
                                  String toEmailAddress,
                                  String messageSubject,
                                  String bodyText)
      throws MessagingException, IOException, GeneralSecurityException {
    
    System.out.println("Sending email...");
    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
    Gmail service =
        new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, agenda.App.getCredentials(HTTP_TRANSPORT))
            .setApplicationName(APPLICATION_NAME)
            .build();
            
    // Encode as MIME message
    Properties props = new Properties();
    Session session = Session.getDefaultInstance(props, null);
    MimeMessage email = new MimeMessage(session);
    email.setFrom(new InternetAddress(fromEmailAddress));
    email.addRecipient(javax.mail.Message.RecipientType.TO,
        new InternetAddress(toEmailAddress));
    email.setSubject(messageSubject);
    email.setContent(bodyText, "text/html");

    // Encode and wrap the MIME message into a gmail message
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    email.writeTo(buffer);
    byte[] rawMessageBytes = buffer.toByteArray();
    String encodedEmail = Base64.encodeBase64URLSafeString(rawMessageBytes);
    Message message = new Message();
    message.setRaw(encodedEmail);

    try {
      // Create send message
      message = service.users().messages().send("me", message).execute();

      System.out.println("Sent!");
      return message;
    } catch (GoogleJsonResponseException e) {
      GoogleJsonError error = e.getDetails();
      if (error.getCode() == 403) {
        System.err.println("Unable to send message: " + e.getDetails());
      } else {
        throw e;
      }
    }
    return null;
  }
}