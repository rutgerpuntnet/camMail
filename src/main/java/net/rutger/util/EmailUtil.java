package net.rutger.util;

import org.apache.log4j.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * Created by rutger on 09-06-16.
 */
public class EmailUtil {

    private static final Logger logger = Logger.getLogger(EmailUtil.class);

    public static void email(final String content, final String subject, final boolean contentIsImageLocation,
                             final boolean debug){
        logger.debug("Email image");
        Properties mailProps = getEmailProperties();
        final String username = mailProps.getProperty("emailUser");
        final String password = mailProps.getProperty("emailPassword");

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(mailProps.getProperty("emailFrom"), "CamMail"));

            InternetAddress[] addresses = InternetAddress.parse(mailProps.getProperty("emailTo"));
            InternetAddress[] recipients;
            if (debug && addresses.length>1) {
                recipients = new InternetAddress[] {addresses[0]};
            } else {
                recipients = addresses;
            }
            message.setRecipients(Message.RecipientType.TO, recipients);

            message.setSubject(subject);


            Multipart mp=new MimeMultipart();

            if (contentIsImageLocation) {
                MimeBodyPart attachment= new MimeBodyPart();
                attachment.attachFile(content);
                mp.addBodyPart(attachment);
            } else {
                MimeBodyPart textPart = new MimeBodyPart();
                textPart.setText(content, "utf-8");
                mp.addBodyPart(textPart);
            }

            message.setContent(mp);

            Transport.send(message);

            System.out.println("Email done");

        } catch (MessagingException e) {
            logger.error("MessagingException while sending email", e);
        } catch (IOException e) {
            logger.error("IOException while creating image for sending email", e);
        }
    }

    private static Properties getEmailProperties() {
        Properties prop = new Properties();
        InputStream input = null;
        try {

            input = new FileInputStream("/var/frontdoor/email.properties");

            // load a properties file
            prop.load(input);
            return prop;
        } catch (IOException ex) {
            logger.error("IOException while loading email props", ex);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return new Properties();
    }


}
