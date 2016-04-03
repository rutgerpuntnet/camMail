package net.rutger;

import org.apache.wicket.request.http.flow.AbortWithHttpErrorCodeException;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import javax.servlet.http.HttpServletResponse;

/**
 * Created by rutger on 03-04-16.
 */
public class EmailPage extends AbstractCamPage {
    private static final String THUMBNAIL_LOCATION = "/var/frontdoor/latestThumbnail.jpg";

    public EmailPage() {
        this(new PageParameters());
    }


    public EmailPage(PageParameters parameters) {
        super(parameters);
        if (emailImage(THUMBNAIL_LOCATION, "1")) {
            throw new AbortWithHttpErrorCodeException(HttpServletResponse.SC_OK);
        }
        throw new AbortWithHttpErrorCodeException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }


}
