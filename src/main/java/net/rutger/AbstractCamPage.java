package net.rutger;

import net.rutger.util.EmailUtil;
import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.DynamicImageResource;
import org.apache.wicket.request.resource.IResource;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
                String subject = "Deurbel ging: " + ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
                EmailUtil.email(imageLocation, subject, true, false);
            }
            return getNonCachingImage(imageLocation);
        } catch (IOException e) {
            logger.error("IOException on ffmpeg call", e);
        }
        return null;
    }

    protected NonCachingImage getNonCachingImage(final String imageLocation) {
        final byte[] imageBytes = getImage(imageLocation);
        logger.debug("image byte length = " + imageBytes.length);

        IResource imageResource = new DynamicImageResource() {
            @Override
            protected byte[] getImageData(Attributes attributes) {
                return imageBytes;
            }
        };
        return new NonCachingImage("thumbnail", imageResource);

    }
    private void runScript(String imageLocation, String param) throws IOException {
        long startmillis = System.currentTimeMillis();

        String[] command = {"/usr/local/bin/frontdoorimage.sh", imageLocation, param};

        ProcessBuilder pb = new ProcessBuilder(command);

        logger.debug("Run command: " + command);
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

    protected boolean email(String imageLocation, String param) {
        try {
            runScript(imageLocation, param);
            String subject = "Deurbel ging: " + ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
            EmailUtil.email(imageLocation, subject, true, false);
        } catch (IOException e) {
            logger.error("IOException on ffmpeg call", e);
            return false;
        }
        return true;
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
