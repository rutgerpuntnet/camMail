package net.rutger;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebApplication;

/**
 * Application object for your web application.
 * If you want to run this application without deploying, run the Start class.
 * 
 * @see net.rutger.Start#main(String[])
 */
public class WicketApplication extends WebApplication
{
	/**
	 * @see org.apache.wicket.Application#getHomePage()
	 */
	@Override
	public Class<? extends WebPage> getHomePage()
	{
		return HomePage.class;
	}

	/**
	 * @see org.apache.wicket.Application#init()
	 */
	@Override
	public void init()
	{
		super.init();
		mount(new MountedMapperWithoutPageComponentInfo("/homepage", HomePage.class));
		mount(new MountedMapperWithoutPageComponentInfo("/highRes", HighResPage.class));
		mount(new MountedMapperWithoutPageComponentInfo("/email", EmailPage.class));

		// add your configuration here
	}
}
