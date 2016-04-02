package net.rutger;

import org.apache.log4j.Logger;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.request.http.WebResponse;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import javax.servlet.http.HttpServletResponse;

public class HomePage extends AbstractCamPage {
	private static final String THUMBNAIL_LOCATION = "/var/frontdoor/latestThumbnail.jpg";
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(HomePage.class);

	public HomePage(final PageParameters parameters) {
		super(parameters);

		NonCachingImage image = processImage(THUMBNAIL_LOCATION, "1");

		Link reloadLink = new Link("reload") {
			@Override
			public void onClick() {
				PageParameters params = new PageParameters();
				params.add("param","true");
				setResponsePage(new HomePage(params));
			}
		};
		reloadLink.add(image);
		this.add(reloadLink);

		BookmarkablePageLink highResLink = new BookmarkablePageLink("highres", HighResPage.class);
		this.add(highResLink);

	}

	//TODO
	// add mail image
	// give image unique datetimestamp name

}
