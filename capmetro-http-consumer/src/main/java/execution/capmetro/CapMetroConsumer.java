package execution.capmetro;

import http.data.PathProperties;
import http.execution.HttpDailyRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;

/**
 * The main application class. The application takes source URLs as input and outputs the body of
 * the http response to some destination. It does so by executing a collection of runnables, each runnable
 * corresponding to one source and one destination. Given a source and other metadata, the runnable must
 * be able to create a unique destination to save the data to with each run.
 */
public class CapMetroConsumer {

    private static final Logger logger = LoggerFactory.getLogger(CapMetroConsumer.class);

    public static void main(String[] args) {

        final long initialDelay = 1;
        final long delay = 30;
        final TimeUnit timeUnit = TimeUnit.SECONDS;
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(4);
        List<Runnable> runners = getRunners();
        List<ScheduledFuture<?>> tasks = new ArrayList<>(4);
        for (Runnable runner : runners) {
            tasks.add(executorService.scheduleWithFixedDelay(runner, initialDelay, delay, timeUnit));
        }
        while (true) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ie) {
                logger.error("Main thread execution interrupted. Exiting application...", ie);
                System.exit(1);
            }
            for (ScheduledFuture<?> task : tasks) {
                if (task.isDone()) {
                    logger.error("Unexpected error during execution. Inspect application log. Exiting...");
                    System.exit(1); //TODO: Add notifier instead of exiting.
                }
            }
        }
    }

    private static List<Runnable> getRunners() {

        final String vehiclePositionsJsonURL = "https://data.austintexas.gov/razzle/cuc7-ywmd/text/plain";
        final String tripUpdatesJsonURL = "https://data.texas.gov/download/mqtr-wwpy/text%2Fplain";
        final String vehiclePositionsPbURL = "https://data.texas.gov/download/eiei-9rpf/application%2Foctet-stream";
        final String tripUpdatesPbURL = "https://data.texas.gov/download/rmk2-acnw/application%2Foctet-stream";

        final String vehiclePositionsPrefix = "vehicle-positions";
        final String tripUpdatesPrefix = "trip-updates";
        final String jsonDirectory = "data" + File.separator + "json";
        final String pbDirectory = "data" + File.separator + "pb";
        final String jsonSuffix = "json";
        final String pbSuffix = "pb";
        final String jsonContentType = "application/octet-stream";
        final String pbContentType = "application/octet-stream";

        Map<String, String> requestProperties = new HashMap<>(3);
        requestProperties.put("Content-Type", jsonContentType);
        requestProperties.put("X-App-Token", "b7mZs9To48yt7Lver4EABPq0j");

        PathProperties pathProperties = new PathProperties(vehiclePositionsPrefix, jsonSuffix,
                                                           jsonDirectory + File.separator + vehiclePositionsPrefix);
        Runnable vehiclePositionsJsonRunner = new HttpDailyRunner(vehiclePositionsJsonURL, requestProperties,
                                                                  pathProperties);

        pathProperties = new PathProperties(tripUpdatesPrefix, jsonSuffix,
                                            jsonDirectory + File.separator + tripUpdatesPrefix);
        Runnable tripUpdatesJsonRunner = new HttpDailyRunner(tripUpdatesJsonURL, requestProperties,
                                                             pathProperties);

        requestProperties.put("Content-Type", pbContentType);

        pathProperties = new PathProperties(vehiclePositionsPrefix, pbSuffix,
                                            pbDirectory + File.separator + vehiclePositionsPrefix);
        Runnable vehiclePositionPbRunner = new HttpDailyRunner(vehiclePositionsPbURL, requestProperties,
                                                               pathProperties);

        pathProperties = new PathProperties(tripUpdatesPrefix, pbSuffix,
                                            pbDirectory + File.separator + tripUpdatesPrefix);
        Runnable tripUpdatesPbRunner = new HttpDailyRunner(tripUpdatesPbURL, requestProperties,
                                                           pathProperties);

        return Arrays.asList(tripUpdatesJsonRunner, vehiclePositionsJsonRunner,
                             vehiclePositionPbRunner, tripUpdatesPbRunner);
    }
}