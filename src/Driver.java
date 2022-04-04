import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.ParetoDistribution;
import org.apache.commons.math3.random.RandomGeneratorFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;

enum Distribution {
    EXPONENTIAL(1),
    PARETO(2);

    private final int value;
    Distribution(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

public class Driver {
    static long randomSeed = 0;   // random seed number
    //gsl_rng        *RandGen;      // global random number generator
    static int numRequests;   // num. of requests seen so far
    static int numFinished;   // num. of requests finished so far
    static int numInService; // number of requests being serviced at the moment
    static int totalRequests;
    static int totalTime;
    static double currentTime = 0; // current time
    static double serverRate;
    static double requestRate;
    static double workLoadMean; // mean of the workload is calculated based on the RequestRate and SystemLoad
    static double paretoAlpha;
    static String cacheType;
    static double institutionBandwidth;
    static double fifoBandWidth;
    static double logNormalMean;
    static double logNormalStd;
    static int numFiles;
    static double meanFileSize;
    // minimum of a Pareto random variable representing the workload size;
    // it is computed based on the input parameter Pareto_Alpha and WorkloadMean
    static double pareto_K;
    static EventPriorityQueue eventPQ;
    static FIFOQueue packetQueue;
    static Cache cache;
    static double cacheSize;

    // the following are used to compute the mean
    static CumulativeMeasurement inService = new CumulativeMeasurement(0.0, 0.0);  // cumulative number in service
    static CumulativeMeasurement queued = new CumulativeMeasurement(0.0, 0.0); // cumulative number in the queue
    static CumulativeMeasurement queueingDelay = new CumulativeMeasurement(0.0, 0.0); // cumulative queueing in the queue
    static CumulativeMeasurement responseTime = new CumulativeMeasurement(0.0, 0.0); // cumulative response time

    static Distribution workLoadDistribution;
    static Random rand = new Random(randomSeed);
    static RandomGenerator rng = RandomGeneratorFactory.createRandomGenerator(rand);
    static LogNormalDistribution logNormalDist;
    static ExponentialDistribution expDist;
    static Map<Integer, FileMetadata> fileMap;



    public static void main(String[] args) throws Exception {
        if( args.length != 2 ) {
            System.out.println(Arrays.toString(args));
            System.out.println( "Usage : javac Driver.java input_file seed_number");
            panic("main() : You'd better learn how to use this program");
        }

        eventPQ = EventPriorityQueue.getInstance();               // event priority queue
        packetQueue = FIFOQueue.getInstance();              // FIFO packet queue

        InputReader inp = new InputReader(args[0]);

        cacheSize = inp.getCacheSize();
        cacheType = inp.getCacheType();
        switch(cacheType) {
            case "LRU":
                cache = new LRUCache(cacheSize); // in MBs
                break;
            default:
                break;
        }
        institutionBandwidth = inp.getInstitutionBandwidth();
        totalRequests = inp.getTotalRequests();
        totalTime = inp.getTotalTime();
        serverRate = inp.getServerRate();
        requestRate = inp.getRequestRate();
        logNormalMean = inp.getLogNormalMean();
        logNormalStd = inp.getLogNormalStd();
        numFiles = inp.getNumberOfFiles();
        fifoBandWidth = inp.getFifoBandWidth();
        meanFileSize = inp.getMeanFileSize();

        workLoadMean = inp.getWorkLoadMean();
        paretoAlpha = inp.getParetoAlpha();
        randomSeed = Long.parseLong(args[1]);
        workLoadDistribution = inp.getWorkLoadDistribution();
        pareto_K = (paretoAlpha-1.0)/paretoAlpha * workLoadMean;
        double logNormalScale = Math.exp(logNormalMean);
        logNormalDist = new LogNormalDistribution(rng, logNormalScale, logNormalStd);
        expDist = new ExponentialDistribution(rng, 1.0/requestRate);
        FileSelection fileSelector = new FileSelection(numFiles, paretoAlpha, meanFileSize, pareto_K, paretoAlpha, rng);
         fileMap = fileSelector.generateFiles();


        System.out.println("Workload Mean: " + workLoadMean);
        System.out.println("Pareto Alpha: " + paretoAlpha);
        System.out.println("Pareto K: " + pareto_K);
        System.out.println("Random seed is " + randomSeed);

        long timeNow = System.currentTimeMillis();
        System.out.println("Simulation started at: " + timeNow);
        double workLoad = getWorkLoad();
        mainLoop();
        System.out.println("Simulation ended at: " + timeNow);
        printStatistics();

    }

    public static double getWorkLoad() {
        double wl = 0.0;
        switch(workLoadDistribution) {
            case EXPONENTIAL:
                ExponentialDistribution e = new ExponentialDistribution(rng, workLoadMean);
                wl = e.sample();
                break;
            case PARETO:
                ParetoDistribution p = new ParetoDistribution(rng, pareto_K, paretoAlpha);
                wl = p.sample();
        }
        return wl;
    }

    public static void mainLoop() throws Exception {
        int randomFileId = 1 + (int)(Math.random() * numFiles);
        Packet packet = new Packet(randomFileId, fileMap.get(randomFileId).getSize(), fileMap.get(randomFileId).getPopularity(), 0);
        Event event = new Event(0, currentTime, packet, packet.getPacketId(), EventType.NEW_REQUEST_EVENT);
        eventPQ.enqueue(event);

        while(numFinished < totalRequests || currentTime < totalTime) {
            if (!eventPQ.isEmpty()) {
                Event e = eventPQ.dequeue();
                currentTime = e.key;
                switch(e.func) {
                    case NEW_REQUEST_EVENT:
                        newRequestEvent(e.getPacket().getPacketId());
                        break;
                    case FILE_RECEIVED_EVENT:
                        fileReceivedEvent(e.getPacket().getRequestTime());
                        break;
                    case ARRIVE_AT_QUEUE_EVENT:
                        arriveAtQueueEvent(e.getPacket().getPacketId(), e.getPacket().getRequestTime());
                        break;
                    case DEPART_QUEUE_EVENT:
                        departQueueEvent(e.getPacket().getPacketId(), e.getPacket().getRequestTime());
                        break;
                }
            }
        }
    }

    public static void newRequestEvent(int fileId) {
        numInService++;
        numRequests++;
        inService.addToCumulatedValue(numInService);
        inService.addToNumPoints(1.0);

        double cumulExecutionTime;
        //check in the cache
        if(cache.get(fileId).getFileId() > 0) {
            cumulExecutionTime = ( fileMap.get(fileId).getSize() / institutionBandwidth);
            double d = currentTime + cumulExecutionTime;
            Event fileReceivedEvent = new Event(0, d, new Packet(fileId, fileMap.get(fileId).getSize(),
                    fileMap.get(fileId).getPopularity(), cumulExecutionTime), fileId, EventType.FILE_RECEIVED_EVENT);
            cache.put(fileId, new FileMetadata(fileMap.get(fileId).getSize(), fileMap.get(fileId).getPopularity()));
            eventPQ.enqueue(fileReceivedEvent);
        }
        else {
            cumulExecutionTime = logNormalDist.sample();
            double d = currentTime + cumulExecutionTime;
            Event cacheMissEvent = new Event(0, d, new Packet(fileId, fileMap.get(fileId).getSize(),
                    fileMap.get(fileId).getPopularity(), cumulExecutionTime), fileId, EventType.ARRIVE_AT_QUEUE_EVENT);
            eventPQ.enqueue(cacheMissEvent);
            d = currentTime + expDist.sample();
            int randomFileId = 1 + (int)(Math.random() * numFiles);
            Packet packet = new Packet(randomFileId, fileMap.get(randomFileId).getSize(), fileMap.get(randomFileId).getPopularity(), 0);
            Event newRequestEvent = new Event(0, d, packet, randomFileId, EventType.NEW_REQUEST_EVENT);
            eventPQ.enqueue(newRequestEvent);

        }
    }

    public static void fileReceivedEvent(double packetRequestTime) {
        responseTime.addToNumPoints(1);
        responseTime.addToCumulatedValue(packetRequestTime);
        numFinished++;
    }

    public static void arriveAtQueueEvent(int fileId, double packetRequestTime) {
        numInService--;
        queued.addToCumulatedValue(packetQueue.getTotalItems());
        queued.addToNumPoints(1.0);
        if(packetQueue.getTotalItems() == 0) {
            double executionTime = (fileMap.get(fileId).getSize() / fifoBandWidth);
            double d = currentTime + executionTime;
            Event departQueueEvent = new Event(0, d, new Packet(fileId, fileMap.get(fileId).getSize(),
                    fileMap.get(fileId).getPopularity(), packetRequestTime + executionTime),
                    fileId, EventType.DEPART_QUEUE_EVENT);
            eventPQ.enqueue(departQueueEvent);
        }
        else {
            packetQueue.enqueue(new Packet(fileId, fileMap.get(fileId).getSize(), fileMap.get(fileId).getPopularity(),
                    packetRequestTime));
        }
    }

    public static void departQueueEvent(int fileId, double packetRequestTime) throws Exception {
        cache.put(fileId, new FileMetadata(fileMap.get(fileId).getSize(), fileMap.get(fileId).getPopularity()));
        double executionTime = fileMap.get(fileId).getSize() / institutionBandwidth;
        double d = currentTime + executionTime;
        queueingDelay.addToCumulatedValue(d - packetRequestTime);
        queueingDelay.addToNumPoints(1.0);
        Event fileReceivedEvent = new Event(0, d, new Packet(
                fileId, fileMap.get(fileId).getSize(), fileMap.get(fileId).getPopularity(), packetRequestTime + executionTime),
                fileId, EventType.FILE_RECEIVED_EVENT);
        eventPQ.enqueue(fileReceivedEvent);
        if(packetQueue.getTotalItems()> 0) {
            Packet headPacket = packetQueue.getHead();
            int newFileId = headPacket.getPacketId();
            executionTime = fileMap.get(newFileId).getSize() / fifoBandWidth;
            d = currentTime + executionTime;
            Event departQueueEvent = new Event(0, d, new Packet(newFileId, fileMap.get(newFileId).getSize(),
                    fileMap.get(newFileId).getPopularity(), packetRequestTime + executionTime), newFileId,
                    EventType.FILE_RECEIVED_EVENT);
            eventPQ.enqueue(departQueueEvent);
            packetQueue.dequeue(headPacket);
        }
    }

    public static double getRequestInterArrivalTime() {
        ExponentialDistribution ed = new ExponentialDistribution(rng, 1.0/requestRate);
        return ed.sample();
    }

    public static void printStatistics() {
        double mean = 0.0;
        System.out.println("****************** Simulation Statistics *******************");
        System.out.println("Total number of requests finished: " + numFinished);
        System.out.println("Total simulated time: " + currentTime);

        if (inService.getNumPoints() != 0.0) {
            mean = inService.getCumulatedValue() / inService.getNumPoints();
            System.out.println("Average number of requests in service: " + mean);
        }

        if (queued.getNumPoints() != 0.0) {
            mean = queued.getCumulatedValue() / queued.getNumPoints();
            System.out.println("Average number of requests in queue: " + mean);
        }

        if (queueingDelay.getNumPoints() != 0.0) {
            mean = queueingDelay.getCumulatedValue() / queueingDelay.getNumPoints();
            System.out.println("Average queueing delay: " + mean);
        }

        if (responseTime.getNumPoints() != 0.0) {
            mean = responseTime.getCumulatedValue() / responseTime.getNumPoints();
            System.out.println("Average response time: " + mean);
        }

    }

    public static void panic(String s) {
        System.out.println("****** Simulator Panic ******");
        System.out.println("   Message : " + s);
        System.exit(1);
    }
}
