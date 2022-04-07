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
//    static double serverRate;
    static double requestRate;
    static double paretoMean; // mean of the workload is calculated based on the RequestRate and SystemLoad
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
        requestRate = inp.getRequestRate();
        logNormalMean = inp.getLogNormalMean();
        logNormalStd = inp.getLogNormalStd();
        numFiles = inp.getNumberOfFiles();
        fifoBandWidth = inp.getFifoBandWidth();
        paretoMean = inp.getParetoMean();

        paretoAlpha = inp.getParetoAlpha();
        randomSeed = Long.parseLong(args[1]);
        pareto_K = (paretoAlpha-1.0)/paretoAlpha * paretoMean;
        logNormalDist = new LogNormalDistribution(rng, logNormalMean, logNormalStd);
        expDist = new ExponentialDistribution(rng, 1.0/requestRate);
        FileSelection fileSelector = new FileSelection(numFiles, paretoAlpha, paretoMean, paretoAlpha, pareto_K, rng);
         fileMap = fileSelector.generateFiles();


        System.out.println("Pareto Mean(Bytes) " + paretoMean);
        System.out.println("Pareto Alpha " + paretoAlpha);
        System.out.println("Pareto K " + pareto_K);
        System.out.println("Total Requests " + totalRequests);
        System.out.println("Number of files " + numFiles);
        System.out.println("Request Rate " + requestRate);
        System.out.println("Log Normal Mean(s) " + logNormalMean);
        System.out.println("Log Normal Standard Deviation(s) " + logNormalStd);
        System.out.println("Cache Type " + cacheType);
        System.out.println("Institution Bandwidth " + institutionBandwidth);
        System.out.println("FIFO Bandwidth " + fifoBandWidth);
        System.out.println("Cache Size " + cacheSize);


        long timeNow = System.currentTimeMillis();
        System.out.println("Simulation started at: " + timeNow);
        mainLoop();
        System.out.println("Simulation ended at: " + timeNow);
        printStatistics();

    }

    public static void mainLoop() throws Exception {
        int randomFileId = 1 + (int)(Math.random() * numFiles);
        Packet packet = new Packet(randomFileId, fileMap.get(randomFileId).getSize(), fileMap.get(randomFileId).getPopularity(), currentTime);
        Event event = new Event(0, currentTime, packet, packet.getPacketId(), EventType.NEW_REQUEST_EVENT);
        eventPQ.enqueue(event);

        while(numFinished < totalRequests || currentTime < totalTime) {
//            System.out.println("Num finished: " + numFinished + ", current time: " + currentTime);
            if (!eventPQ.isEmpty()) {
                Event e = eventPQ.dequeue();
                currentTime = e.key;
                switch(e.func) {
                    case NEW_REQUEST_EVENT:
                        newRequestEvent(e.getPacket());
                        break;
                    case FILE_RECEIVED_EVENT:
                        fileReceivedEvent(e.getPacket().getRequestCreationTime());
                        break;
                    case ARRIVE_AT_QUEUE_EVENT:
                        arriveAtQueueEvent(e.getPacket());
                        break;
                    case DEPART_QUEUE_EVENT:
                        departQueueEvent(e.getPacket());
                        break;
                }
            }
        }
    }

    public static void newRequestEvent(Packet packet) {
        numInService++;
        numRequests++;
        inService.addToCumulatedValue(numInService);
        inService.addToNumPoints(1.0);
        double d;

        //check in the cache
        int fileId = packet.getPacketId();
        if(cache.get(fileId).getFileId() > 0) {
            d = currentTime + ( fileMap.get(fileId).getSize() / institutionBandwidth);
            Event fileReceivedEvent = new Event(0, d, packet, fileId, EventType.FILE_RECEIVED_EVENT);
            cache.put(fileId, new FileMetadata(fileMap.get(fileId).getSize(), fileMap.get(fileId).getPopularity()));
            eventPQ.enqueue(fileReceivedEvent);
        }
        else {
            d = currentTime + logNormalDist.sample();
            Event cacheMissEvent = new Event(0, d, packet, fileId, EventType.ARRIVE_AT_QUEUE_EVENT);
            eventPQ.enqueue(cacheMissEvent);
            if(numRequests < totalRequests) {
                d = currentTime + expDist.sample();
                int randomFileId = 1 + (int)(Math.random() * numFiles);
                Packet newPacket = new Packet(randomFileId, fileMap.get(randomFileId).getSize(), fileMap.get(randomFileId).getPopularity(), currentTime);
                Event newRequestEvent = new Event(0, d, newPacket, randomFileId, EventType.NEW_REQUEST_EVENT);
                eventPQ.enqueue(newRequestEvent);
            }
        }
    }

    public static void fileReceivedEvent(double packetCreationTime) {
        responseTime.addToNumPoints(1);
        responseTime.addToCumulatedValue(currentTime - packetCreationTime);
        numFinished++;
    }

    public static void arriveAtQueueEvent(Packet packet) {
        numInService--;
        queued.addToCumulatedValue(packetQueue.getTotalItems());
        queued.addToNumPoints(1.0);
        int fileId = packet.getPacketId();
        if(packetQueue.getTotalItems() == 0) {
            double d = currentTime + (fileMap.get(fileId).getSize() / fifoBandWidth);
            Event departQueueEvent = new Event(0, d, packet, fileId, EventType.DEPART_QUEUE_EVENT);
            eventPQ.enqueue(departQueueEvent);
        }
        else {
            packetQueue.enqueue(packet);
        }
    }

    public static void departQueueEvent(Packet packet) throws Exception {
        int fileId = packet.getPacketId();
        cache.put(fileId, new FileMetadata(fileMap.get(fileId).getSize(), fileMap.get(fileId).getPopularity()));
        double d = currentTime + fileMap.get(fileId).getSize() / institutionBandwidth;
//        queueingDelay.addToCumulatedValue(d - packet.getRequestCreationTime());
//        queueingDelay.addToNumPoints(1.0);
        Event fileReceivedEvent = new Event(0, d, packet, fileId, EventType.FILE_RECEIVED_EVENT);
        eventPQ.enqueue(fileReceivedEvent);
        if(packetQueue.getTotalItems()> 0) {
            Packet headPacket = packetQueue.getHead();
            int newFileId = headPacket.getPacketId();
            d = currentTime + fileMap.get(newFileId).getSize() / fifoBandWidth;
            Event departQueueEvent = new Event(0, d, headPacket, newFileId, EventType.FILE_RECEIVED_EVENT);
            eventPQ.enqueue(departQueueEvent);
            packetQueue.dequeue(headPacket);
        }
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
