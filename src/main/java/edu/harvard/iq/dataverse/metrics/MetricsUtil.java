package edu.harvard.iq.dataverse.metrics;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

public class MetricsUtil {

    private static final Logger logger = Logger.getLogger(MetricsUtil.class.getCanonicalName());

    // TODO: Should this really be hard-coded to "Locale.US"?
    private final static Locale LOCALE = Locale.US;
    private final static String MONTH = "Month";
    private final static String MONTH_NUM = "monthNum";
    private final static String NAME = "name";
    private final static String RUNNING_TOTAL = "running_total";
    private final static String MONTH_SORT = "month_sort";
    private final static String DISPLAY_NAME = "display_name";
    private final static String VALUE = "value";
    private final static String WEIGHT = "weight";
    private final static String TYPE = "type";
    private final static String LABEL = "label";

    public static JsonArrayBuilder downloadsToJson(List<Object[]> listOfObjectArrays) {
        JsonArrayBuilder jab = Json.createArrayBuilder();
        if (listOfObjectArrays.size() < 1) {
            return jab;
        }
        // Skip first item in list, which contains a total;
        for (Object[] objectArray : listOfObjectArrays.subList(1, listOfObjectArrays.size())) {
            JsonObjectBuilder job = Json.createObjectBuilder();
            Timestamp dateString = (Timestamp) objectArray[0];
            logger.fine("dateString: " + dateString);
            LocalDate localDate = LocalDate.parse(dateString.toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S"));
            long numDownloadsPerMonth = (long) objectArray[1];
            logger.fine("numDownloadsPerMonth: " + numDownloadsPerMonth);
            String numDownloadsPerMonthFriendly = NumberFormat.getNumberInstance(LOCALE).format(numDownloadsPerMonth);
            String monthYear = localDate.getMonth().getDisplayName(TextStyle.FULL, LOCALE) + " " + localDate.getYear();
            job.add(MONTH, monthYear);
            int monthNum = localDate.getMonthValue();
            job.add(MONTH_NUM, monthNum);
            String name = "Total File Downloads";
            job.add(NAME, name);
            job.add("Number of File Downloads", numDownloadsPerMonth);
            // FIXME: This running total is a place holder. It should be increasing over time.
            long runningTotal = Long.MAX_VALUE;
            logger.fine("runningTotal: " + runningTotal);
            String runningTotalFriendly = NumberFormat.getNumberInstance(LOCALE).format(runningTotal);
            job.add(RUNNING_TOTAL, runningTotal);
            String monthSort = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            job.add(MONTH_SORT, monthSort);
            String displayName = monthYear + ": " + numDownloadsPerMonthFriendly + " downloads / total: " + runningTotalFriendly;
            job.add(DISPLAY_NAME, displayName);
            jab.add(job);
        }
        return jab;
    }

    static JsonArrayBuilder dataversesByMonthToJson(List<Object[]> listOfObjectArrays) {
        JsonArrayBuilder jab = Json.createArrayBuilder();
        for (Object[] objectArray : listOfObjectArrays) {
            JsonObjectBuilder job = Json.createObjectBuilder();
            Timestamp dateString = (Timestamp) objectArray[0];
            logger.fine("dateString: " + dateString);
            LocalDate localDate = LocalDate.parse(dateString.toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S"));
            long numDataversesCreated = (long) objectArray[1];
            logger.fine("numDataversesCreated: " + numDataversesCreated);
            BigDecimal runningTotal = (BigDecimal) objectArray[2];
            logger.fine("runningTotal: " + runningTotal);
            String numDataversesCreatedFriendly = NumberFormat.getNumberInstance(LOCALE).format(numDataversesCreated);
            String monthYear = localDate.getMonth().getDisplayName(TextStyle.FULL, LOCALE) + " 2017";
            job.add(MONTH, monthYear);
            int monthNum = localDate.getMonthValue();
            job.add(MONTH_NUM, monthNum);
            String name = "Total Dataverses";
            job.add(NAME, name);
            job.add("Number of Dataverses", numDataversesCreated);
            job.add("running_total", runningTotal);
            String monthSort = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            job.add(MONTH_SORT, monthSort);
            String runningTotalFriendly = NumberFormat.getNumberInstance(LOCALE).format(runningTotal);
            // TODO: For consistency shouldn't runningTotalFriendly be included in display_name?
            // TODO: For consistency shouldn't it be "new" instead of "New" in display_name?
            String displayName = monthYear + ": " + numDataversesCreatedFriendly + " New Dataverses";
            job.add(DISPLAY_NAME, displayName);
            jab.add(job);
        }
        return jab;
    }

    static JsonArrayBuilder datasetsByMonthToJson(List<Object[]> listOfObjectArrays) {
        JsonArrayBuilder jab = Json.createArrayBuilder();
        for (Object[] objectArray : listOfObjectArrays) {
            JsonObjectBuilder job = Json.createObjectBuilder();
            Timestamp dateString = (Timestamp) objectArray[0];
            logger.fine("dateString: " + dateString);
            LocalDate localDate = LocalDate.parse(dateString.toString(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S"));
            long numDatasetsCreated = (long) objectArray[1];
            logger.fine("numDatasetsCreated: " + numDatasetsCreated);
            BigDecimal runningTotal = (BigDecimal) objectArray[2];
            logger.fine("runningTotal: " + runningTotal);
            String numDatasetsCreatedFriendly = NumberFormat.getNumberInstance(LOCALE).format(numDatasetsCreated);
            String monthYear = localDate.getMonth().getDisplayName(TextStyle.FULL, LOCALE) + " 2017";
            job.add(MONTH, monthYear);
            int monthNum = localDate.getMonthValue();
            job.add(MONTH_NUM, monthNum);
            String name = "Total Datasets";
            job.add(NAME, name);
            job.add("Number of Datasets", numDatasetsCreated);
            job.add("running_total", runningTotal);
            String monthSort = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            job.add(MONTH_SORT, monthSort);
            String runningTotalFriendly = NumberFormat.getNumberInstance(LOCALE).format(runningTotal);
            String displayName = monthYear + ": " + numDatasetsCreatedFriendly + " new Datasets; Total of " + runningTotalFriendly;
            job.add(DISPLAY_NAME, displayName);
            jab.add(job);
        }
        return jab;
    }

    static JsonArrayBuilder datasetsBySubjectToJson(List<Object[]> listOfObjectArrays) {
        JsonArrayBuilder jab = Json.createArrayBuilder();
        float totalDatasets = 0;
        for (Object[] objectArray : listOfObjectArrays) {
            long value = (long) objectArray[1];
            totalDatasets = totalDatasets + value;
        }
        for (Object[] objectArray : listOfObjectArrays) {
            String type = (String) objectArray[0];
            long value = (long) objectArray[1];
            // FIXME: Too much precision. Should be "0.006", for example.
            double weight = value / totalDatasets;
            JsonObjectBuilder job = Json.createObjectBuilder();
            job.add(VALUE, value);
            job.add(WEIGHT, weight);
            job.add("totalDatasets", totalDatasets);
            job.add(TYPE, type);
            String percentage = String.format("%.1f", value * 100f / totalDatasets) + "%";
            job.add(LABEL, type + " (" + percentage + ")");
            jab.add(job);
        }
        return jab;
    }

}
