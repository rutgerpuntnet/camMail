package net.rutger;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.DynamicImageResource;
import org.apache.wicket.request.resource.IResource;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.imageio.ImageIO;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.ServletContext;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Locale;
import java.util.Properties;

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
                emailImage(imageLocation);
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
        Integer errCode;
        ProcessBuilder pb = new ProcessBuilder("/usr/local/bin/frontdoorimage.sh", imageLocation, param);
        logger.debug("Run command");
        Process process = pb.start();
        logger.debug(getProcessOutput(process));
        try {
            errCode = process.waitFor();
            logger.debug("Image encoding done:" + errCode);
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

    protected boolean emailImage(String imageLocation, String param) {
        try {
            runScript(imageLocation, param);
            emailImage(imageLocation);
        } catch (IOException e) {
            logger.error("IOException on ffmpeg call", e);
            return false;
        }
        return true;
    }

    private void emailImage(String imageLocation){
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

            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(mailProps.getProperty("emailTo")));

            DateTimeFormatter fmt = DateTimeFormat.forPattern("E d MMMM, HH:mm").withLocale(new Locale("nl", "NL"));
            message.setSubject("Deurbel ging: " + new DateTime().toString(fmt));

            Multipart mp=new MimeMultipart();

            MimeBodyPart attachment= new MimeBodyPart();
            attachment.attachFile(imageLocation);
            mp.addBodyPart(attachment);

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
        File imageFile = new File(fileNamePath);
        BufferedImage img = null;
        byte[] bytes;
        try {
            img = ImageIO.read(imageFile);
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
