package net.rutger;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TimerServlet extends HttpServlet {

    private static final String KNMI_BASE_URL = "http://http://projects.knmi.nl/klimatologie/daggegevens/getdata_dag.cgi?stns=240&vars=PRCP&start=$DATESTRING&end=$DATESTRING";
    private static final String GARDEN_ARDUINO_BASE_URL = "http://192.168.1.12";
    private static final String GARDEN_ARDUINO_TIMER_PARAMETER = "?setTimer=";
    private static final int DEFAULT_WATER_MINUTES = 10;

    private static final Logger logger = Logger.getLogger(TimerServlet.class);

    public void init() throws ServletException {
        logger.debug("Starting TimerServlet");

        TimerTask gardenWaterTask = new TimerTask(){

            @Override
            public void run() {
                runGardenWaterTask();
            }
        };

        Timer sevenAmtimer = new Timer();
        sevenAmtimer.schedule(gardenWaterTask, getNextTime(7), TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS));
        logger.debug("TimerServlet has started");

    }

    private void runGardenWaterTask() {
        // Get data from KNMI (for now, start with default 10 minutes
        int minutes = DEFAULT_WATER_MINUTES;

        // Get arduino URL
        BufferedReader in = null;
        try {
            URL arduinoUrl = new URL(GARDEN_ARDUINO_BASE_URL + GARDEN_ARDUINO_TIMER_PARAMETER + minutes);
            arduinoUrl.openStream();
        } catch (IOException e) {
            logger.error("Exception on calling arduino URL", e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
            throws ServletException, IOException {
        // Nothing to do here, we only use this servlet for the init method to start the timer
    }


    public void destroy() {
        // do nothing.
        logger.debug("Destroy TimerServlet");
    }

    public long getNextTime(int hourOfDay) {
        DateTime dt = new DateTime().withTime(hourOfDay, 0, 0, 0);
        return dt.getMillis();
    }
}