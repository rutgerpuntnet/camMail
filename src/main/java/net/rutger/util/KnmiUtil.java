package net.rutger.util;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by rutger on 09-06-16.
 */
public class KnmiUtil {
    private static final Logger logger = Logger.getLogger(KnmiUtil.class);

    private static final String KNMI_BASE_URL = "http://projects.knmi.nl/klimatologie/daggegevens/getdata_dag.cgi?stns=240&vars=PRCP&start=DATESTRING&end=DATESTRING";

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
        Map<WeatherDataTypes, Integer> result = new HashMap<>();

        String[] lines = rawData.split("\\s*\\r?\\n\\s*");
        Integer contentLineIndex = determineContentLineNumber(lines);
        if(contentLineIndex != null) {
            // get the columnames, these are 2 lines above the data.
            String[] columnames = lines[contentLineIndex-2].replaceAll("\\s+","").split(",");
            String[] columvalues = lines[contentLineIndex].replaceAll("\\s+","").split(",");
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
