import DBSCAN.DBSCAN;
import com.sun.org.apache.bcel.internal.generic.BREAKPOINT;
import com.sun.org.apache.xpath.internal.axes.IteratorPool;
import net.sf.javaml.clustering.Clusterer;
import net.sf.javaml.clustering.DensityBasedSpatialClustering;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.Instance;
import net.sf.javaml.core.SparseInstance;
import org.jfree.chart.ChartColor;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.util.ShapeUtilities;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.io.*;
import java.time.*;
import java.util.*;
import java.util.List;


/**
 * Created by Sparsh Bansal on 4/3/2016.
 */
public class Simulator {

    /**
     * Declaring the basic constants for generating simulation data
     * The constants define the probabilities and graphing data
     * <p>
     * The person is assumed to be living in an rectangular area with some max Limit
     * <p>
     * Since we will graph the data we are assuming the coordinate system as follows :-
     * The top left corner of the window is the coordinate (0,0)
     * The bottom right corner of the window is the coordinate (1000,1000)
     * <p>
     * Consider each step to be of 1 meter so the living area is a 1km * 1Km grid
     */

    // The maximum x and y of the living area
    private static final int AREA_WIDTH = 1000;
    private static final int AREA_HEIGHT = 1000;

    // Latitude Longitude information for each point on the grid
    private static final double[][] latitude = new double[AREA_WIDTH][AREA_HEIGHT];
    private static final double[][] longitude = new double[AREA_WIDTH][AREA_HEIGHT];

    // The coordinate of the person's home
    private static final int X_HOME = 879;
    private static final int Y_HOME = 901;

    // The coordinate of the person's work place
    private static final int X_WORK = 306;
    private static final int Y_WORK = 257;

    // The Latitude and Longitude of the top left corner
    private static final double LATITUDE_TOP_LEFT = 26.651212;
    private static final double LONGITUDE_TOP_LEFT = 76.228002;

    // The working hours of the person will be from morning 9:00 AM to 5:00 PM
    // Just for simulation puposes we are considering the starting date to be 01/01/2016 (dd/mm/yyyy)
    // The starting time and date variable for sampling
    private static final LocalDateTime FIRST_DAY = LocalDateTime.of(2001, 1, 1, 1, 0, 0);


    private static final LocalTime WORK_TIME_START = LocalTime.of(9, 0, 0);
    private static final LocalTime WORK_TIME_END = LocalTime.of(17, 0, 0);

    private static final LocalTime LEAVING_TIME = LocalTime.of(8, 0, 0);
    private static final LocalTime ARRIVING_TIME = LocalTime.of(18, 0, 0);

    // The Number of hours of work for the person
    private static final int WORKING_HOURS = 8;

    // The ideal time it takes for him to commute between office and work (in minutes)
    private static final int BASE_CONVEYANCE_TIME = 60;

    private static int NUM_DAYS = 0;
    private static int SAMPLING_PERIOD = 0;

    // Enumeration used for generating the route
    private enum Steps {
        LEFT, RIGHT, UP, DOWN
    }

    private static final List<Steps> ROUTE = new ArrayList<>();

    /**
     * Defining constant for various probabilities , to add randomness to the behaviour of the simulator
     * <p>
     * Probability of being at work in work hours -- 85% == 0.85
     * Probability of being at home when not at work in work hours -- 60%
     * Probability of being somewhere else in work hours when not at work -- 40%
     */
    private static final float P_WORK_HOURS_WORK = 0.85f;
    private static final float P_WORK_HOURS_HOME = 0.15f * 0.60f;
    private static final float P_WORK_HOURS_SOMEPLACE = 0.15f * 0.40f;
    private static final float P_HOME_HOURS_HOME = 0.90f;
    private static final float P_HOME_HOURS_SOMEPLACE = 0.10f;

    /**
     * Defining a lot of noise constants because obviously there is got to be some noise!!
     * <p>
     * Noise in measuring location -- Noise will be calculated by generating a random radius lets say < 5 and
     * placing the point within the circle centered at the place at which the person currently is.
     */
    private static final int MAX_LOCATION_NOISE_RADIUS = 50;
    private static final int MAX_LOCATION_NOISE_WHILE_TRAVELLING = 5;

    /**
     * Max Noise in the time of leaving for work place and arriving at work place (in minutes)
     * <p>
     * These noise will determine random traffic conditions , although the speed of the person will be assumed
     * constant throughout his journey to work and back home
     */
    private static final int MAX_LEAVING_FROM_HOME_TIME_NOISE = 10;
    private static final int MAX_ARRIVING_AT_WORK_TIME_NOISE = 10;
    private static final int MAX_LEAVING_FROM_WORK_TIME_NOISE = 10;
    private static final int MAX_ARRIVING_AT_HOME_TIME_NOISE = 10;

    /**
     * Noise for each day will be generated at the start of the day and will be constant for that day except
     * for the error in location measurement
     * <p>
     * The following instance variables store the day regarding the current day
     */
    private static LocalDateTime currentDayDateTime;

    // Boolean to store whether the person will go to work today or not
    private static boolean willGoToWork;
    private static boolean willStayAtHome;

    private static LocalTime leavingFromHomeTime;
    private static LocalTime arrivingAtWorkTime;
    private static LocalTime leavingFromWorkTime;
    private static LocalTime arrivingAtHomeTime;

    public static Vector<Point> locationVector = new Vector<>();

    public static final int KEY_X_COORDINATE_ATTRIBUTE = 0;
    public static final int KEY_Y_COORDINATE_ATTRIBUTE = 1;

    public Simulator() {

    }

    /**
     * Helper method to generate a random route from home to work and back , Route will remain constant for
     * one run of simulation.
     */
    private static void generateRoute() {

        ROUTE.clear();

        if (X_HOME < X_WORK) {
            for (int i = 0; i < X_WORK - X_HOME; i++) {
                ROUTE.add(Steps.RIGHT);
            }
        } else {
            for (int i = 0; i < X_HOME - X_WORK; i++) {
                ROUTE.add(Steps.LEFT);
            }
        }

        if (Y_HOME < Y_WORK) {
            for (int i = 0; i < Y_WORK - Y_HOME; i++) {
                ROUTE.add(Steps.DOWN);
            }
        } else {
            for (int i = 0; i < Y_HOME - Y_WORK; i++) {
                ROUTE.add(Steps.UP);
            }
        }

        // Now shuffle the collection to generate a random route
        Collections.shuffle(ROUTE);
    }

    /**
     * Helper Method to generate the noise constants for the current day
     */
    private static void generateValuesForCurrentDay() {
        // We're gonna use the random class to generate random constants
        Random noise = new Random();

        // If Today is not a weekend day then we generate the data
        if (currentDayDateTime.getDayOfWeek() != DayOfWeek.SATURDAY ||
                currentDayDateTime.getDayOfWeek() != DayOfWeek.SUNDAY) {

            willGoToWork = (Math.random() <= P_WORK_HOURS_WORK);
            // If the person will go to work , then we are gonna generate the noise constants
            if (willGoToWork) {

                int leaving_from_home_time_noise = noise.nextInt(MAX_LEAVING_FROM_HOME_TIME_NOISE);
                int arriving_at_work_time_noise = noise.nextInt(MAX_ARRIVING_AT_WORK_TIME_NOISE);
                int leaving_from_work_time_noise = noise.nextInt(MAX_LEAVING_FROM_WORK_TIME_NOISE);
                int arriving_at_home_time_noise = noise.nextInt(MAX_ARRIVING_AT_HOME_TIME_NOISE);

                Random random = new Random();

                boolean willLeaveHomeEarly = (random.nextInt(2) == 1);
                boolean willArriveAtWorkEarly = (random.nextInt(2) == 1);
                boolean willLeaveWorkEarly = (random.nextInt(2) == 1);
                boolean willArriveAtHomeEarly = (random.nextInt(2) == 1);

                if (willLeaveHomeEarly) {
                    leavingFromHomeTime = LEAVING_TIME.minusMinutes(leaving_from_home_time_noise);
                } else {
                    leavingFromHomeTime = LEAVING_TIME.plusMinutes(leaving_from_home_time_noise);
                }

                if (willArriveAtWorkEarly) {
                    arrivingAtWorkTime = WORK_TIME_START.minusMinutes(arriving_at_work_time_noise);
                } else {
                    arrivingAtWorkTime = WORK_TIME_START.plusMinutes(arriving_at_work_time_noise);
                }

                if (willLeaveWorkEarly) {
                    leavingFromWorkTime = WORK_TIME_END.minusMinutes(leaving_from_work_time_noise);
                } else {
                    leavingFromWorkTime = WORK_TIME_END.plusMinutes(leaving_from_work_time_noise);
                }

                if (willArriveAtHomeEarly) {
                    arrivingAtHomeTime = ARRIVING_TIME.minusMinutes(arriving_at_home_time_noise);
                } else {
                    arrivingAtHomeTime = ARRIVING_TIME.plusMinutes(arriving_at_home_time_noise);
                }
            }
            // If the person is not at work today , then we generate whether he is at home or not
            else {
                willStayAtHome = (Math.random() <= P_WORK_HOURS_HOME);
            }
        } else {
            willGoToWork = false;
            willStayAtHome = (Math.random() <= P_HOME_HOURS_HOME);
        }
    }

    private static void initialize() {
        currentDayDateTime = FIRST_DAY;
        generateRoute();
        generateValuesForCurrentDay();
        generateLatitudeLongitudeLocations();
    }

    public static void main(String args[]) {

        initialize();
        Scanner input = new Scanner(System.in);

        System.out.println("Enter the number of Days you want to sample : ");
        NUM_DAYS = input.nextInt();

        System.out.println("Enter the sampling rate in minutes : ");
        SAMPLING_PERIOD = input.nextInt();

        System.out.println("Starting Simulation....");
        SimulationThread simulation = new SimulationThread();
        simulation.start();
        try {
            simulation.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Simulation Ended...");

        // Create instances from the dataset for the learning algorithm
        Dataset mlDataset = createMLDatasetFromFile();

        // Big test time , lets feed the data into the algorithm
        Clusterer dbscanClusterer = new DBSCAN(0.01, 4);
        Dataset[] clusters = dbscanClusterer.cluster(mlDataset);

        XYSeriesCollection graphData = new XYSeriesCollection();

        // Now lets plot the clusters onto the graph
        for (int i = 0; i < clusters.length; i++) {
            XYSeries currentCluster = new XYSeries("Cluster " + i);
            Iterator<Instance> clusterIterator = clusters[i].iterator();

            while (clusterIterator.hasNext()) {
                Instance currentInstance = clusterIterator.next();
                currentCluster.add(currentInstance.get(KEY_X_COORDINATE_ATTRIBUTE), currentInstance.get(KEY_Y_COORDINATE_ATTRIBUTE));
            }
            graphData.addSeries(currentCluster);
        }

        JFreeChart chart = ChartFactory.createScatterPlot(
                "Location Plot",
                "Latitude",
                "Longitude",
                graphData,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        Shape ellipse2D = new Ellipse2D.Double(0, 0, 2, 2);


        XYPlot xyPlot = (XYPlot) chart.getPlot();
        XYItemRenderer renderer = xyPlot.getRenderer();
        renderer.setSeriesShape(0, ellipse2D);

        for (int i = 0; i < clusters.length; i++) {
            Random random = new Random();
            Paint paint = new ChartColor(random.nextInt(256), random.nextInt(256), random.nextInt(256));
            renderer.setSeriesShape(i, ellipse2D);
            renderer.setSeriesPaint(i, paint);
        }

        ChartFrame chartFrame = new ChartFrame("Location Chart Frame", chart);
        chartFrame.pack();
        chartFrame.setVisible(true);

    }

    private static void generateLatitudeLongitudeLocations() {
        // We fill the data of the corresponding lat long info.
        latitude[0][0] = LATITUDE_TOP_LEFT;
        longitude[0][0] = LONGITUDE_TOP_LEFT;

        final double dx = 0.01;
        final double dy = 0.01;

        for (int i = 0; i < AREA_HEIGHT; i++) {
            for (int j = 1; j < AREA_WIDTH; j++) {
                latitude[i][j] = latitude[i][j - 1];
                longitude[i][j] = longitude[i][j - 1] + ((dx / 111) / Math.cos(latitude[i][j - 1] * (Math.PI / 180)));
            }
            if (i < AREA_HEIGHT - 1) {
                latitude[i + 1][0] = latitude[i][0] + (dy / 111);
                longitude[i + 1][0] = longitude[i][0];
            }
        }
    }

    private static Dataset createMLDataset() {

        Dataset mlDataset = new DefaultDataset();
        Iterator<Point> iterator = locationVector.iterator();

        while (iterator.hasNext()) {
            Instance instance = new SparseInstance(2);
            Point currentPoint = iterator.next();

            instance.put(KEY_X_COORDINATE_ATTRIBUTE, currentPoint.x);
            instance.put(KEY_Y_COORDINATE_ATTRIBUTE, currentPoint.y);

            mlDataset.add(instance);
        }

        return mlDataset;
    }

    private static XYDataset createDataset() {

        System.out.println("Location Data Number of Points : " + locationVector.size());

        {
            Iterator<Point> iterator = locationVector.iterator();
            while (iterator.hasNext()) {
                final Point currentPoint = iterator.next();
                System.out.println(currentPoint.x + " " + currentPoint.y);
            }
        }

        XYSeriesCollection result = new XYSeriesCollection();
        XYSeries series = new XYSeries("Location Data");
        Iterator<Point> iterator = locationVector.iterator();
        while (iterator.hasNext()) {
            Point p = iterator.next();
            series.add(p.x, p.y);
        }
        result.addSeries(series);
        return result;
    }

    private static XYDataset createDatasetFromFile() {
        File file = new File("C:\\Users\\Sparsh Bansal\\Downloads\\SHAREit\\2014818\\file\\Location_Data_File");

        XYSeriesCollection result = new XYSeriesCollection();
        XYSeries series = new XYSeries("Location Data");

        if (file.exists()) {

            try {
                FileInputStream fis = new FileInputStream(file);
                DataInputStream dis = new DataInputStream(fis);
                double latitude;
                double longitude;
                while ((latitude = dis.readDouble()) != -1) {
                    longitude = dis.readDouble();
                    series.add(latitude, longitude);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            result.addSeries(series);
        }
        return result;
    }

    private static Dataset createMLDatasetFromFile() {
        Dataset mlDataset = new DefaultDataset();
        File file = new File("C:\\Users\\Sparsh Bansal\\Downloads\\SHAREit\\2014818\\file\\Location_Data_File");

        if (file.exists()) {

            try {
                FileInputStream fis = new FileInputStream(file);
                DataInputStream dis = new DataInputStream(fis);
                double latitude;
                double longitude;
                while ((latitude = dis.readDouble()) != -1) {
                    longitude = dis.readDouble();

                    Instance instance = new SparseInstance(2);

                    instance.put(KEY_X_COORDINATE_ATTRIBUTE, latitude);
                    instance.put(KEY_Y_COORDINATE_ATTRIBUTE, longitude);
                    mlDataset.add(instance);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return mlDataset;
    }

    // Simulation will be performed on separate thread
    public static class SimulationThread extends Thread {


        @Override
        public void run() {

            final int loop_limit = (NUM_DAYS * 24 * 60) / SAMPLING_PERIOD;
            for (int i = 0; i < loop_limit; i++) {

                LocalTime currentTime = currentDayDateTime.toLocalTime();

                // Check whether we go to work today or not
                if (willGoToWork) {

                    // From midnight to before leaving for work we are at home
                    if (currentTime.isAfter(LocalTime.of(0, 0, 0)) &&
                            currentTime.isBefore(leavingFromHomeTime)) {

                        int randomTheta = new Random().nextInt(361);
                        double noiseRadius = new Random().nextInt(MAX_LOCATION_NOISE_RADIUS);
                        double xCoordinate =
                                (X_HOME + noiseRadius * Math.cos(Math.toRadians(randomTheta)));
                        double yCoordinate =
                                (Y_HOME + noiseRadius * Math.sin(Math.toRadians(randomTheta)));

                        final double currentLatitude = latitude[(int) xCoordinate][(int) yCoordinate];
                        final double currentLongitude = longitude[(int) xCoordinate][(int) yCoordinate];
                        Point currentLocation = new Point(currentLatitude, currentLongitude);
                        locationVector.add(currentLocation);
                    }

                    // If the current Time is between our conveyance time , so we generate according data points on the
                    // route , traffic condition are automatically simulated by noise in leaving and arriving time.

                    // The speed is assumed to be constant during that period
                    if (currentTime.isAfter(leavingFromHomeTime) && currentTime.isBefore(arrivingAtWorkTime)) {
                        long timeElapsed = Duration.between(leavingFromHomeTime, currentTime).toMinutes();
                        long totalConveyanceTime = Duration.between(leavingFromHomeTime, arrivingAtWorkTime).toMinutes();

                        double fractionOfRouteCovered = (float) timeElapsed / (float) totalConveyanceTime;
                        int numSteps = (int) (fractionOfRouteCovered * ROUTE.size());

                        Iterator<Steps> itr = ROUTE.iterator();
                        double xCoordinate = X_HOME;
                        double yCoordinate = Y_HOME;

                        while (itr.hasNext() && numSteps > 0) {
                            switch (itr.next()) {
                                case LEFT:
                                    xCoordinate--;
                                    break;
                                case RIGHT:
                                    xCoordinate++;
                                    break;
                                case UP:
                                    yCoordinate--;
                                    break;
                                case DOWN:
                                    yCoordinate++;
                                    break;
                            }
                            numSteps--;
                        }
                        final int locationNoise = new Random().nextInt(MAX_LOCATION_NOISE_WHILE_TRAVELLING + 1);
                        final int randomTheta = new Random().nextInt(361);
                        xCoordinate = xCoordinate + (locationNoise * Math.cos(Math.toRadians(randomTheta)));
                        yCoordinate = yCoordinate + (locationNoise * Math.sin(Math.toRadians(randomTheta)));

                        final double currentLatitude = latitude[(int) xCoordinate][(int) yCoordinate];
                        final double currentLongitude = longitude[(int) xCoordinate][(int) yCoordinate];
                        Point currentLocation = new Point(currentLatitude, currentLongitude);
                        locationVector.add(currentLocation);
                    }

                    // While the person is at work , the data is generated around the WORK Point
                    if (currentTime.isAfter(arrivingAtWorkTime) && currentTime.isBefore(leavingFromWorkTime)) {
                        int randomTheta = new Random().nextInt(361);
                        int noiseRadius = new Random().nextInt(MAX_LOCATION_NOISE_RADIUS);
                        double xCoordinate =
                                (X_WORK + noiseRadius * Math.cos(Math.toRadians(randomTheta)));
                        double yCoordinate =
                                (Y_WORK + noiseRadius * Math.sin(Math.toRadians(randomTheta)));

                        final double currentLatitude = latitude[(int) xCoordinate][(int) yCoordinate];
                        final double currentLongitude = longitude[(int) xCoordinate][(int) yCoordinate];
                        Point currentLocation = new Point(currentLatitude, currentLongitude);
                        locationVector.add(currentLocation);
                    }


                    // The Time period between leaving from Work and Arriving at Home.
                    if (currentTime.isAfter(leavingFromWorkTime) && currentTime.isBefore(arrivingAtHomeTime)) {

                        long timeElapsed = Duration.between(leavingFromWorkTime, currentTime).toMinutes();
                        long totalConveyanceTime = Duration.between(leavingFromWorkTime, arrivingAtHomeTime).toMinutes();

                        double fractionOfRouteCovered = (float) timeElapsed / (float) totalConveyanceTime;
                        int numSteps = (int) (fractionOfRouteCovered * ROUTE.size());

                        ListIterator<Steps> itr = ROUTE.listIterator(ROUTE.size());
                        double xCoordinate = X_WORK;
                        double yCoordinate = Y_WORK;

                        while (itr.hasPrevious() && numSteps > 0) {
                            switch (itr.previous()) {
                                case LEFT:
                                    xCoordinate++;
                                    break;
                                case RIGHT:
                                    xCoordinate--;
                                    break;
                                case UP:
                                    yCoordinate++;
                                    break;
                                case DOWN:
                                    yCoordinate--;
                                    break;
                            }
                            numSteps--;
                        }
                        final int locationNoise = new Random().nextInt(MAX_LOCATION_NOISE_WHILE_TRAVELLING + 1);
                        final int randomTheta = new Random().nextInt(361);
                        xCoordinate = xCoordinate + (locationNoise * Math.cos(Math.toRadians(randomTheta)));
                        yCoordinate = yCoordinate + (locationNoise * Math.sin(Math.toRadians(randomTheta)));

                        final double currentLatitude = latitude[(int) xCoordinate][(int) yCoordinate];
                        final double currentLongitude = longitude[(int) xCoordinate][(int) yCoordinate];
                        Point currentLocation = new Point(currentLatitude, currentLongitude);
                        locationVector.add(currentLocation);
                    }

                    // After arriving at home and before leaving for work.
                    if (currentTime.isAfter(arrivingAtHomeTime) && currentTime.isBefore(LocalTime.of(0, 0, 0))) {

                        int randomTheta = new Random().nextInt(361);
                        double noiseRadius = new Random().nextInt(MAX_LOCATION_NOISE_RADIUS);
                        double xCoordinate =
                                (X_HOME + noiseRadius * Math.cos(Math.toRadians(randomTheta)));
                        double yCoordinate =
                                (Y_HOME + noiseRadius * Math.sin(Math.toRadians(randomTheta)));

                        final double currentLatitude = latitude[(int) xCoordinate][(int) yCoordinate];
                        final double currentLongitude = longitude[(int) xCoordinate][(int) yCoordinate];
                        Point currentLocation = new Point(currentLatitude, currentLongitude);
                        locationVector.add(currentLocation);
                    }
                } else {
                    // From midnight to before leaving for work we are at home
                    if (currentDayDateTime.toLocalTime().isAfter(LocalTime.of(0, 0, 0)) &&
                            currentDayDateTime.toLocalTime().isBefore(leavingFromHomeTime)) {

                        int randomTheta = new Random().nextInt(361);
                        double noiseRadius = new Random().nextInt(MAX_LOCATION_NOISE_RADIUS);
                        double xCoordinate =
                                (X_HOME + noiseRadius * Math.cos(Math.toRadians(randomTheta)));
                        double yCoordinate =
                                (Y_HOME + noiseRadius * Math.sin(Math.toRadians(randomTheta)));


                        final double currentLatitude = latitude[(int) xCoordinate][(int) yCoordinate];
                        final double currentLongitude = longitude[(int) xCoordinate][(int) yCoordinate];
                        Point currentLocation = new Point(currentLatitude, currentLongitude);
                        locationVector.add(currentLocation);
                    }
                    // If he is going to stay at home we generate according data
                    if (willStayAtHome) {
                        int randomTheta = new Random().nextInt(361);
                        double noiseRadius = new Random().nextInt(MAX_LOCATION_NOISE_RADIUS);
                        double xCoordinate =
                                (X_HOME + noiseRadius * Math.cos(Math.toRadians(randomTheta)));
                        double yCoordinate =
                                (Y_HOME + noiseRadius * Math.sin(Math.toRadians(randomTheta)));


                        final double currentLatitude = latitude[(int) xCoordinate][(int) yCoordinate];
                        final double currentLongitude = longitude[(int) xCoordinate][(int) yCoordinate];
                        Point currentLocation = new Point(currentLatitude, currentLongitude);
                        locationVector.add(currentLocation);
                    }

                    // When not at home , generate points randomly
                    else {
                        double xCoordinate = new Random().nextInt(1000);
                        double yCoordinate = new Random().nextInt(1000);

                        final double currentLatitude = latitude[(int) xCoordinate][(int) yCoordinate];
                        final double currentLongitude = longitude[(int) xCoordinate][(int) yCoordinate];
                        Point currentLocation = new Point(currentLatitude, currentLongitude);
                        locationVector.add(currentLocation);
                    }
                }
                // Now we increment the currentDayDateTime by sampling period
                if (currentDayDateTime.plusMinutes(SAMPLING_PERIOD).getDayOfWeek() !=
                        currentDayDateTime.getDayOfWeek()) {
                    generateValuesForCurrentDay();
                }
                currentDayDateTime = currentDayDateTime.plusMinutes(SAMPLING_PERIOD);
            }

        }
    }
}
