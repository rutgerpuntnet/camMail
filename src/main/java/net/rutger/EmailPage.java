package net.rutger;

import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.http.WebRequest;
import org.apache.wicket.request.http.flow.AbortWithHttpErrorCodeException;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import javax.servlet.http.HttpServletRequest;
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
        if (getPageParameters().get("debug").toBoolean()) {
            email(getPageParameters().get("content").toString("Test email"), false, true);
            throw new AbortWithHttpErrorCodeException(HttpServletResponse.SC_OK);
        } else {
            WebRequest req = (WebRequest) RequestCycle.get().getRequest();
            HttpServletRequest httpReq = (HttpServletRequest) req.getContainerRequest();
            String clientAddress = httpReq.getRemoteAddr();
            if (clientAddress.startsWith("192.168.")) {
                if (email(THUMBNAIL_LOCATION, "1")) {
                    throw new AbortWithHttpErrorCodeException(HttpServletResponse.SC_OK);
                }
            } else {
                throw new AbortWithHttpErrorCodeException(HttpServletResponse.SC_FORBIDDEN);
            }

        }

        throw new AbortWithHttpErrorCodeException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }


}
