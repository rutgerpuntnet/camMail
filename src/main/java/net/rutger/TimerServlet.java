package net.rutger;

import net.rutger.wateringsystem.WateringTimerTask;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TimerServlet extends HttpServlet {


    private static final Logger logger = Logger.getLogger(TimerServlet.class);

    public void init() throws ServletException {
        logger.debug("Starting TimerServlet");

        TimerTask hourlyTask = new TimerTask(){

            @Override
            public void run() {
                logger.debug("Running hourly task at " + ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME));
                // TODO implement hourly task (retrieving and storing data from watersystem)
            }
        };


        Timer sevenAmtimer = new Timer();
        ZonedDateTime nextTimeSevenAm = getNextTime(7);
        sevenAmtimer.schedule(new WateringTimerTask(), Date.from(nextTimeSevenAm.toInstant()), TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS));
        logger.debug("WateringTimerTask (daily) set for " + nextTimeSevenAm.format(DateTimeFormatter.RFC_1123_DATE_TIME));

        Timer hourlyTimer = new Timer();
        hourlyTimer.schedule(hourlyTask, new Date(), TimeUnit.MILLISECONDS.convert(1, TimeUnit.HOURS));
        logger.debug("TimerServlet has started");


    }

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
            throws ServletException, IOException {
        // Nothing to do here, we only use this servlet for the init method to start the timer

        // Temporary run the watertask on a get request (testing purposes)
        logger.debug("Run the watertask");
        new WateringTimerTask().runGardenWaterTask();
    }


    public void destroy() {
        // do nothing.
        logger.debug("Destroy TimerServlet");
    }

    public ZonedDateTime getNextTime(int hourOfDay) {
        ZonedDateTime dt = ZonedDateTime.now().withHour(hourOfDay).withMinute(0).withSecond(0);
        if (dt.isBefore(ZonedDateTime.now())) {
            dt = dt.plusDays(1);
        }
        return dt;
    }
}