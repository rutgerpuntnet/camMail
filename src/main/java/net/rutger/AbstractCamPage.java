package net.rutger;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.DynamicImageResource;
import org.apache.wicket.request.resource.IResource;

import javax.imageio.ImageIO;
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
import javax.servlet.ServletContext;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Created by rutger on 05-03-16.
 */
public abstract class AbstractCamPage extends WebPage {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(AbstractCamPage.class);


    public AbstractCamPage() {

    }

    public AbstractCamPage(PageParameters parameters) {
        super(parameters);
    }

    protected NonCachingImage processImage(String imageLocation, String param) {

        try {
            runScript(imageLocation, param);

            if(getPageParameters().get("email").toBoolean()){
                email(imageLocation, true, false);
            }

            final byte[] imageBytes = getImage(imageLocation);
            logger.debug("image byte length = " + imageBytes.length);

            IResource imageResource = new DynamicImageResource() {
                @Override
                protected byte[] getImageData(Attributes attributes) {
                    return imageBytes;
                }
            };
            return new NonCachingImage("thumbnail", imageResource);
        } catch (IOException e) {
            logger.error("IOException on ffmpeg call", e);
        }
        return null;
    }

    private void runScript(String imageLocation, String param) throws IOException {
        long startmillis = System.currentTimeMillis();

        String[] command = {"/usr/local/bin/frontdoorimage.sh", imageLocation, param};

        ProcessBuilder pb = new ProcessBuilder(command);

        logger.debug("Run command");
        Process process = pb.start();
        logger.debug("Process output: " + getProcessOutput(process));
        try {
            boolean error = process.waitFor(20l, TimeUnit.SECONDS);
            logger.debug("Done running command:" + error);
            logger.debug("Exitcode " + process.exitValue());
        } catch(InterruptedException ex) {
            logger.error("InterruptedException on waiting for script", ex);
            Thread.currentThread().interrupt();
        }

        long totalMillis = System.currentTimeMillis() - startmillis;
        logger.debug("ffmpeg call in ms:" + totalMillis);

        try {
            Thread.sleep(100);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

    }

    protected boolean email(String imageLocation, String param) {
        try {
            runScript(imageLocation, param);
            email(imageLocation, true, false);
        } catch (IOException e) {
            logger.error("IOException on ffmpeg call", e);
            return false;
        }
        return true;
    }

    protected void email(String content, boolean contentIsImageLocation, boolean debug){
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
            message.setFrom(new InternetAddress(mailProps.getProperty("emailFrom"), "Voordeur"));

            InternetAddress[] addresses = InternetAddress.parse(mailProps.getProperty("emailTo"));
            InternetAddress[] recipients;
            if (debug && addresses.length>1) {
                recipients = new InternetAddress[] {addresses[0]};
            } else {
                recipients = addresses;
            }
            message.setRecipients(Message.RecipientType.TO, recipients);

            message.setSubject("Deurbel ging: " + ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME));


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

    private Properties getEmailProperties() {
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

    protected byte[] getImage(String fileNamePath) {
        byte[] bytes;
        try {
            BufferedImage img = ImageIO.read(new File(fileNamePath));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "jpg", baos);
            bytes = baos.toByteArray();
        } catch (IOException e) {
            logger.error("failed to read image");
            return new byte[0];
        }
        return bytes;
    }

    protected static ServletContext getContext(){

        WebApplication webApplication = WebApplication.get();
        if(webApplication!=null){
            ServletContext servletContext = webApplication.getServletContext();
            if(servletContext!=null){
                return servletContext;
            }else{
                //do nothing
            }
        }else{
            //do nothing
        }

        return null;

    }

    private String getProcessOutput(Process process) throws IOException {
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getInputStream()));
        StringBuilder builder = new StringBuilder();
        String line = null;
        while ( (line = reader.readLine()) != null) {
            builder.append(line);
            builder.append(System.getProperty("line.separator"));
        }
        return builder.toString();
    }
}
