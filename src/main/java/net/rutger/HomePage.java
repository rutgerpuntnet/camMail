package net.rutger;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.resource.DynamicImageResource;
import org.apache.wicket.request.resource.IResource;


import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Date;

public class HomePage extends WebPage {
	private static final String THUMBNAIL_LOCATION = "/tmp/imageFile.jpg";
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(HomePage.class);

	public HomePage(final PageParameters parameters) {
		super(parameters);

		try {
			long startmillis = System.currentTimeMillis();

			ProcessBuilder pb = new ProcessBuilder("frontdoorimage.sh", THUMBNAIL_LOCATION, "1");
			logger.debug("Run command");
			Process process = pb.start();
			int errCode = process.waitFor();


			logger.debug("Encoding done:" + errCode);

			long totalmillis = System.currentTimeMillis() - startmillis;
			logger.debug("ffmpeg call in ms:" + totalmillis);

			final byte[] thumbnail = getImage(THUMBNAIL_LOCATION);
			logger.debug("Thumbnail byte length = " + thumbnail.length);

			IResource imageResource = new DynamicImageResource() {
				@Override
				protected byte[] getImageData(IResource.Attributes attributes) {
					return thumbnail;
				}
			};
			Image image = new Image("thumbnail", imageResource);

			Link reloadLink = new Link("reload") {
				@Override
				public void onClick() {
					setResponsePage(new HomePage(new PageParameters()));
				}
			};
			reloadLink.add(image);
			this.add(reloadLink);
		} catch (IOException e) {
			logger.error("IOException on ffmpeg call", e);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}


	private byte[] getImage(String fileNamePath) {
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

	public static ServletContext getContext(){

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
}
