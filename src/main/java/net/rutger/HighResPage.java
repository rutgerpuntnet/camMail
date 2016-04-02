package net.rutger;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.DynamicImageResource;
import org.apache.wicket.request.resource.IResource;

import javax.imageio.ImageIO;
import javax.servlet.ServletContext;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class HighResPage extends AbstractCamPage {
	private static final String THUMBNAIL_LOCATION = "/var/frontdoor/latestHighRes.jpg";
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(HighResPage.class);

	public HighResPage(final PageParameters parameters) {
		super(parameters);


		Image image = processImage(THUMBNAIL_LOCATION, "0");

		Link reloadLink = new Link("reload") {
			@Override
			public void onClick() {
				setResponsePage(new HomePage(new PageParameters()));
			}
		};
		reloadLink.add(image);
		this.add(reloadLink);
		BookmarkablePageLink lowResLink = new BookmarkablePageLink("lowres", HomePage.class);
		this.add(lowResLink);
	}
}
