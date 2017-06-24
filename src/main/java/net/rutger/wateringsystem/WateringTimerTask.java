package net.rutger.wateringsystem;

import net.rutger.util.EmailUtil;
import net.rutger.util.KnmiUtil;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TimerTask;

/**
 * Created by rutger on 18-05-16.
 */
public class WateringTimerTask extends TimerTask {
    private static final Logger logger = Logger.getLogger(WateringTimerTask.class);
    private static final String GARDEN_ARDUINO_BASE_URL = "http://192.168.1.12";
    private static final String GARDEN_ARDUINO_TIMER_PARAMETER = "?setTimer=";
    private static final int DEFAULT_WATER_MINUTES = 10;
    private static final int AVERAGE_MAKKINK = 27;


    @Override
    public void run() {
        try {
            if (isWateringsystemActive()) {
                runGardenWaterTask();
            } else {
                logger.debug("Wateringsystem deactivated");
            }
        } catch (RuntimeException e) {
            logger.error("RuntimException on running watering task");
        }
    }


    public static void runGardenWaterTask() {
        logger.debug("Running garden water task at " + ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME));
        int minutes = determineWateringMinutes();

        // Get request on arduino wateringsystem
        try {
            if (minutes > 0) {
                Map<String,Integer> wateringSystemData = callWateringSystem(minutes);
            }
        } catch (IOException e) {
            logger.error("Exception on calling arduino URL", e);
            String emailContent = "An exeption was caught while trying to call the watering system Arduino. Message: " + e.getMessage();
            EmailUtil.email(emailContent,"Wateringsystem unreachable",false,true);
        }

    }


    public static Map<String,Integer> callWateringSystem(Integer minutes) throws IOException {
        String url = GARDEN_ARDUINO_BASE_URL;
        if (minutes != null) {
            url += GARDEN_ARDUINO_TIMER_PARAMETER + String.valueOf(minutes);
        }
        JSONObject response = new JSONObject(IOUtils.toString(new URL(url), Charset.forName("UTF-8")));

        Map<String, Integer> result = new HashMap<>();
        for (String key : response.keySet()) {
            logger.debug("Json key/value: " + key + " / " + response.getInt(key));
            result.put(key, response.getInt(key));
        }

        return result;

    }

    /*
     * Determine the number of minutes to water, based on weather data and default values
     */
    public static int determineWateringMinutes() {
        // Get data from KNMI (for now, start with default 10 minutes
        int minutes = DEFAULT_WATER_MINUTES;
        try {
            Map<KnmiUtil.WeatherDataTypes, Integer> weatherData = KnmiUtil.getYesterdaysWeatherData();
            Integer transpiration = weatherData.get(KnmiUtil.WeatherDataTypes.TRANSPIRATION);
            logger.info("transpiration " + transpiration);
            // transpiration index is between about 4 (on a very wet day) and about 57 (on a very hot sunny dry day)
            // We'll use 27 as average, any 2 points, we'll add 1 minute, below 25 is the opposite
            if (transpiration != null) {
                int restTranspiration = (transpiration - AVERAGE_MAKKINK) / 2;
                minutes += restTranspiration;
            }
        } catch (RuntimeException e) {
            logger.warn("Exception on receiving weatherdata. Returning default minutes: " + e.getMessage());
        }
        logger.info("Number of minutes to water: " + minutes);

        return minutes;
    }

    private boolean isWateringsystemActive() {
        try (InputStream input = new FileInputStream("app.properties")) {
            Properties properties = new Properties();
            properties.load(input);
            return "true".equalsIgnoreCase(properties.getProperty("wateringsystem.enabled"));
        } catch (IOException ex) {
            logger.error("Cannot read property 'wateringsystem.enabled' ", ex);
        }
        return false;
    }
}
