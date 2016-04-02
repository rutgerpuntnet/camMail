package net.rutger;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.DynamicImageResource;
import org.apache.wicket.request.resource.IResource;

import javax.imageio.ImageIO;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.servlet.ServletContext;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Properties;

/**
 * Created by rutger on 05-03-16.
 */
public abstract class AbstractCamPage extends WebPage {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(AbstractCamPage.class);


    public AbstractCamPage(PageParameters parameters) {
        super(parameters);
    }

    protected NonCachingImage processImage(String imageLocation, String param) {
        long startmillis = System.currentTimeMillis();
        Integer errCode = null;
        try {
            ProcessBuilder pb = new ProcessBuilder("/usr/local/bin/frontdoorimage.sh", imageLocation, param);
            logger.debug("Run command");
            Process process = pb.start();
            logger.debug(getProcessOutput(process));
            errCode = process.waitFor();

        } catch (IOException e) {
            logger.error("IOException on ffmpeg call", e);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        logger.debug("Encoding done:" + errCode);

        long totalMillis = System.currentTimeMillis() - startmillis;
        logger.debug("ffmpeg call in ms:" + totalMillis);

        try {
            Thread.sleep(100);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        final byte[] thumbnail = getImage(imageLocation);
        logger.debug("Thumbnail byte length = " + thumbnail.length);
        if(getPageParameters().get("email").toBoolean()){
            emailImage(thumbnail);
        }

        IResource imageResource = new DynamicImageResource() {
            @Override
            protected byte[] getImageData(Attributes attributes) {
                return thumbnail;
            }
        };
        return new NonCachingImage("thumbnail", imageResource);
    }

    protected void emailImage(byte[] thumbnail){
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
            message.setFrom(new InternetAddress(mailProps.getProperty("emailFrom")));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(mailProps.getProperty("emailTo")));
            message.setSubject("Deurbel");
            message.setText("Er is iemand aan de deur");


            MimeBodyPart attachment= new MimeBodyPart();

            Multipart mp=new MimeMultipart();

            ByteArrayDataSource src = new ByteArrayDataSource
                    (thumbnail,"image/jpeg");
            attachment.setFileName("voordeur.jpg");
            attachment.setContent(src,"image/jpeg");
            mp.addBodyPart(attachment);
            message.setContent(mp);

            Transport.send(message);

            System.out.println("Email done");

        } catch (MessagingException e) {
            logger.error("MessagingException while sending email", e);
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
