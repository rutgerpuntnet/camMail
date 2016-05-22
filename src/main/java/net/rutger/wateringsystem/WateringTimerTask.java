package net.rutger.wateringsystem;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

/**
 * Created by rutger on 18-05-16.
 */
public class WateringTimerTask extends TimerTask {
    private static final Logger logger = Logger.getLogger(WateringTimerTask.class);
    private static final String KNMI_BASE_URL = "http://projects.knmi.nl/klimatologie/daggegevens/getdata_dag.cgi?stns=240&vars=PRCP&start=DATESTRING&end=DATESTRING";
    private static final String GARDEN_ARDUINO_BASE_URL = "http://192.168.1.12";
    private static final String GARDEN_ARDUINO_TIMER_PARAMETER = "?setTimer=";
    private static final int DEFAULT_WATER_MINUTES = 10;
    private static final int AVERAGE_MAKKINK = 27;

    public enum WeatherDataTypes {
        TRANSPIRATION("EV24"),
        PRECIP_DURATION("DR"),
        PRECIP_MILLIMETER("RH"),
        PRECIP_MAX_HOUR_MILLIMETER("RHX");

        private static HashMap<String, WeatherDataTypes> codeValueMap = new HashMap<String, WeatherDataTypes>(2);

        static {
            for (WeatherDataTypes type : WeatherDataTypes.values()) {
                codeValueMap.put(type.code, type);
            }
        }

        private final String code;
        
        WeatherDataTypes(String code) {
            this.code = code;
        }

        public static WeatherDataTypes getByCode(String code) {
            return codeValueMap.get(code);
        }
    }
    
    @Override
    public void run() {
        logger.debug("Running garden water task at " + LocalDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME));

        runGardenWaterTask();
    }


    public static void runGardenWaterTask() {
        int minutes = determineWateringMinutes();

        // Get request on arduino wateringsystem
        try {
            if (minutes > 0) {
                Map<String,Integer> wateringSystemData = callWateringSystem(minutes);
            }
        } catch (IOException e) {
            logger.error("Exception on calling arduino URL", e);
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
            logger.debug("Json key: " + key);
            result.put(key, response.getInt(key));
        }

        return result;

    }

    /*
     * Determine the number of minutes to water, based on weather data and default values
     */
    public static int determineWateringMinutes() {
        // TODO implement
        // Get data from KNMI (for now, start with default 10 minutes
        int minutes = DEFAULT_WATER_MINUTES;

        Map<WeatherDataTypes, Integer> weatherData = getYesterdaysWeatherData();
        Integer transpiration = weatherData.get(WeatherDataTypes.TRANSPIRATION);
        logger.info("transpiration " + transpiration);
        // transpiration index is between about 4 (on a very wet day) and about 57 (on a very hot sunny dry day)
        // We'll use 27 as average, any 2 points, we'll add 1 minute, below 25 is the opposite
        if (transpiration != null) {
            int restTranspiration = (transpiration - AVERAGE_MAKKINK) / 2;
            minutes += restTranspiration;
        }
        return minutes;
    }


    /*
     * Get the weather data (from yesterday, which is the latest)
     */
    public static Map<WeatherDataTypes, Integer> getYesterdaysWeatherData(){
        String rawData = getKnmiRawDataFromYesterday();
        return parseWeatherData(rawData);
    }

    /*
     * Parse the given raw weather data from KNMI into a map per dataType
     */
    private static Map<WeatherDataTypes, Integer> parseWeatherData(String rawData) {
        String[] lines = rawData.split("\\s*\\r?\\n\\s*");
        Integer contentLineIndex = determineContentLineNumber(lines);
        // get the columnames, these are 2 lines above the data.
        String[] columnames = lines[contentLineIndex-2].replaceAll("\\s+","").split(",");
        String[] columvalues = lines[contentLineIndex].replaceAll("\\s+","").split(",");
        Map<WeatherDataTypes, Integer> result = new HashMap<>();
        if (columnames.length != columvalues.length) {
            logger.error("wrong data retrieved. No results:\n" + lines);
        } else {
            for (int i = 0; i < columnames.length; i++) {
                WeatherDataTypes type = WeatherDataTypes.getByCode(columnames[i]);
                try {
                    result.put(type, Integer.valueOf(columvalues[i]));
                } catch (NumberFormatException ne) {
                    logger.warn("Unable to parse data from type " + type.name());
                }
            }
        }
        return result;
    }

    /*
     * The raw data from KNMI consists of a bunch of lines, starting with comment about the given data (starting with
     * a # sign). The number of the first (and only) line with actual data is returned by this method
     */
    private static Integer determineContentLineNumber(String[] lines) {
        boolean foundContent = false;
        int contentLineIndex = 0;
        for (String line : lines) {
            if (!line.startsWith("#")) {
                foundContent = true;
                break;
            }
            contentLineIndex++;
        }
        return foundContent ? contentLineIndex : null;
    }

    /*
     * Retrieve the precipitation data from the KNMI of yesterday using their open REST api.
     * We do this for weatherstation 240 (Schiphol airport)
     */
    private static String getKnmiRawDataFromYesterday() {
        InputStream is = null;
        try {
            String formattedDayYesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE);
            String url = KNMI_BASE_URL.replaceAll("DATESTRING",formattedDayYesterday);
            logger.debug("Calling: " + url);
            is = new URL(url).openStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));

            return readAll(rd);
        } catch (IOException e) {
            logger.error("Exception while reading inputstream", e);
        } finally {
            try {
                is.close();
            } catch (NullPointerException e) {
            } catch (IOException e) {
                logger.error("Exception while closing inputstream");
            }
        }
        return "";
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }
}
