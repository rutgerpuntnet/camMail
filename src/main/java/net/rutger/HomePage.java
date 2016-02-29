package net.rutger;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.WebPage;

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
			int exitValue = process.waitFor();
			long totalmillis = System.currentTimeMillis() - startmillis;
			System.out.println("ffmpeg call in ms:" + totalmillis + " with exit value " + exitValue);

		} catch (IOException e) {
			System.out.println("IOException on ffmpeg call");
		} catch (InterruptedException e) {
			System.out.println("InterruptedException on ffmpeg call");
		}

	}
}
