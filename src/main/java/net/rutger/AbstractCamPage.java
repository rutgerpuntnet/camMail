package net.rutger;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.DynamicImageResource;
import org.apache.wicket.request.resource.IResource;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * Created by rutger on 05-03-16.
 */
public abstract class AbstractCamPage extends WebPage {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(AbstractCamPage.class);


    public AbstractCamPage() {
    }

    public AbstractCamPage(IModel<?> model) {
        super(model);
    }

    public AbstractCamPage(PageParameters parameters) {
        super(parameters);
    }

    protected Image processImage(String imageLocation, String param) {
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

        final byte[] thumbnail = getImage(imageLocation);
        logger.debug("Thumbnail byte length = " + thumbnail.length);

        IResource imageResource = new DynamicImageResource() {
            @Override
            protected byte[] getImageData(Attributes attributes) {
                return thumbnail;
            }
        };
        return new Image("thumbnail", imageResource);
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
