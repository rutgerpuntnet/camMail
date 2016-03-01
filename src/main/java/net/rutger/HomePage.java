package net.rutger;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.WebPage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Date;

public class HomePage extends WebPage {
	private static final long serialVersionUID = 1L;

	public HomePage(final PageParameters parameters) {
		super(parameters);

		add(new Label("version", getApplication().getFrameworkSettings().getVersion()));

		// TODO Add your page's components here

		try {
			long startmillis = System.currentTimeMillis();
			Process process = Runtime.getRuntime().exec("/Users/rutger/Downloads/FFMpeg/ffmpeg -y -i rtsp://192.168.1.10:554/user=admin_password=tlJwpbo6_channel=1_stream=0.sdp?real_stream -ss 00:00:01.500 -f image2 -vframes 1 /tmp/thumb.jpg");

			// TODO check/wait for new date/time in file
//			File tempFile=new File("/tmp/thumb.jpg");
//			File renamedFile=new File("/tmp/thumbLatest.jpg");
//			while(!tempFile.renameTo(renamedFile)); //try re-naming the file which is being encoded by ffmpeg
//			Date date = new Date(file.lastModified());

			long lastmodified = System.currentTimeMillis();
			try {
				Thread.sleep(100);
			} catch(InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			while(lastmodified <= System.currentTimeMillis()) {
				File file = new File("/tmp/thumb.jpg");
				lastmodified = file.lastModified();
			}


			System.out.println("Encoding done");

			long totalmillis = System.currentTimeMillis() - startmillis;
			System.out.println("ffmpeg call in ms:" + totalmillis);

		} catch (IOException e) {
			System.out.println("IOException on ffmpeg call");
		}

	}


	private byte[] getImage(File imageFile) {
		BufferedImage img = null;
		byte[] bytes;
		try {
			img = ImageIO.read(imageFile);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(img, "jpg", baos);
			bytes = baos.toByteArray();
		} catch (IOException e) {
			System.out.println("failed to read image");
			return null;
		}
		return bytes;
	}
}
