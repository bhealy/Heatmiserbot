/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package heatmiserbot;

import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class Email {
  public static void send(String userName,String apppassword,String msg,String body) {

    final String password = ""; // app password for heatmiserbot

    Properties props = new Properties();
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true"); 
    props.put("mail.smtp.host", "smtp.gmail.com");
    props.put("mail.smtp.port", "587");

    Session session = Session.getInstance(props,
      new javax.mail.Authenticator() {
        protected PasswordAuthentication getPasswordAuthentication() {
          return new PasswordAuthentication(userName, apppassword);
        }
      });


    try {
      Message message = new MimeMessage(session);
      message.setFrom(new InternetAddress("robertjohnpatrickhealy@gmail.com"));
      message.setRecipients(Message.RecipientType.TO,
        InternetAddress.parse("robertjohnpatrickhealy@gmail.com"));
      message.setSubject(msg);
      message.setText(body);

      Transport.send(message);

    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
  }
}
