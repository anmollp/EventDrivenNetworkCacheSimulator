import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.random.RandomGeneratorFactory;

import java.util.*;

//enum Distribution {
//    EXPONENTIAL(1),
//    PARETO(2);
//
//    private final int value;
//    Distribution(int value) {
//        this.value = value;
//    }
//
//    public int getValue() {
//        return value;
//    }
//}

public class Driver {
    static long randomSeed = 1;   // random seed number
    static int numRequests;   // num. of requests seen so far
    static int numFinished;   // num. of requests finished so far
    static int numInService; // number of requests being serviced at the moment
    static int totalRequests;
    static int totalTime;
    static double currentTime = 0; // current time
    static double requestRate;
    static double paretoMean;
    static double paretoAlpha;
    static String cacheType;
    static double institutionBandwidth;
    static double fifoBandWidth;
    static double logNormalMean;
    static double logNormalStd;
    static int numFiles;
    static double pareto_K;
    static EventPriorityQueue eventPQ;
    static FIFOQueue packetQueue;
    static Cache cache;
    static double cacheSize;
    static int oneByte = 8;
    static double[] popularityProbabilities;
    static double totalFileSize;

    // the following are used to compute the mean
    static CumulativeMeasurement inService = new CumulativeMeasurement(0.0, 0.0);  // cumulative number in service
    static CumulativeMeasurement queued = new CumulativeMeasurement(0.0, 0.0); // cumulative number in the queue
    static CumulativeMeasurement queueingDelay = new CumulativeMeasurement(0.0, 0.0); // cumulative queueing in the queue
    static CumulativeMeasurement responseTime = new CumulativeMeasurement(0.0, 0.0); // cumulative response time
    static CumulativeMeasurement cacheHit = new CumulativeMeasurement(0.0, 0.0); // number of cache hits
    static CumulativeMeasurement cacheMiss = new CumulativeMeasurement(0.0, 0.0); // number of cache misses

    static Random rand = new Random(randomSeed);
    static RandomGenerator rng = RandomGeneratorFactory.createRandomGenerator(rand);
    static LogNormalDistribution logNormalDist;
    static ExponentialDistribution expDist;
    static EnumeratedIntegerDistribution fileSelectDistribution;
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
            case "LP":
                cache = new LPCache(cacheSize); // in MBs
                break;
            case "FIFO":
                cache = new FIFOCache(cacheSize);
            default:
                break;
        }
        institutionBandwidth = inp.getInstitutionBandwidth() / oneByte;
        totalRequests = inp.getTotalRequests();
        totalTime = inp.getTotalTime();
        requestRate = inp.getRequestRate();
        logNormalMean = inp.getLogNormalMean();
        logNormalStd = inp.getLogNormalStd();
        numFiles = inp.getNumberOfFiles();
        fifoBandWidth = inp.getFifoBandWidth() / oneByte;
        paretoMean = inp.getParetoMean();

        paretoAlpha = inp.getParetoAlpha();
        randomSeed = Long.parseLong(args[1]);
        pareto_K = (paretoAlpha-1.0)/paretoAlpha * paretoMean;
        logNormalDist = new LogNormalDistribution(rng, logNormalMean, logNormalStd);
        expDist = new ExponentialDistribution(rng, 1.0/requestRate);
        FileSelection fileSelector = new FileSelection(numFiles, paretoAlpha, paretoMean, paretoAlpha, pareto_K, rng);
        fileMap = fileSelector.generateFiles();
        Integer[] fileIds = fileMap.keySet().toArray(new Integer[0]);
        int[] intArray = Arrays.stream(fileIds).mapToInt(Integer::intValue).toArray();
        FileMetadata[] fileMetas = fileMap.values().toArray(new FileMetadata[0]);
        popularityProbabilities = new double[fileMetas.length];
        for(int i=0; i<fileMetas.length; i++) {
            popularityProbabilities[i] = fileMetas[i].getProbability();
        }
        fileSelectDistribution = new EnumeratedIntegerDistribution(intArray, popularityProbabilities);

        for(FileMetadata fm : fileMetas) {
            totalFileSize += fm.getSize();
        }


        System.out.println("Pareto Mean(MB)=" + paretoMean);
        System.out.println("Pareto Alpha=" + paretoAlpha);
        System.out.println("Pareto K=" + pareto_K);
        System.out.println("Total Requests=" + totalRequests);
        System.out.println("Number of files=" + numFiles);
        System.out.println("Request Rate=" + requestRate);
        System.out.println("Log Normal Mean(secs)=" + logNormalMean);
        System.out.println("Log Normal Standard Deviation(secs)=" + logNormalStd);
        System.out.println("Cache Type=" + cacheType);
        System.out.println("Institution Bandwidth(Mbps)=" + institutionBandwidth * oneByte);
        System.out.println("FIFO Bandwidth(Mbps)=" + fifoBandWidth * oneByte);
        System.out.println("Cache Size(MB)=" + cacheSize);
        System.out.println("Total File Size(MB)=" + totalFileSize);

        long timeNow = System.currentTimeMillis();
        System.out.println("Simulation started at: " + timeNow);
        mainLoop();
        timeNow = System.currentTimeMillis();
        System.out.println("Simulation ended at: " + timeNow);
        printStatistics();

    }

    public static void mainLoop() throws Exception {
        int randomFileId = fileSelectDistribution.sample();
        Packet packet = new Packet(randomFileId,
                fileMap.get(randomFileId).getSize(),
                fileMap.get(randomFileId).getPopularity(),
                fileMap.get(randomFileId).getProbability(),
                currentTime,
                currentTime);
        Event event = new Event(0, currentTime,
                packet,
                packet.getPacketId(),
                EventType.NEW_REQUEST_EVENT);
        eventPQ.enqueue(event);

        //numFinished < totalRequests ||
        while(currentTime < totalTime) {
            if (!eventPQ.isEmpty()) {
                Event e = eventPQ.dequeue();
                currentTime = e.key;
                switch(e.func) {
                    case NEW_REQUEST_EVENT:
                        newRequestEvent(e);
                        break;
                    case FILE_RECEIVED_EVENT:
                        fileReceivedEvent(e);
                        break;
                    case ARRIVE_AT_QUEUE_EVENT:
                        arriveAtQueueEvent(e);
                        break;
                    case DEPART_QUEUE_EVENT:
                        departQueueEvent(e);
                        break;
                }
            }
        }
    }

    public static void newRequestEvent(Event ev) {
        numInService++;
        numRequests++;
        inService.addToCumulatedValue(numInService);
        inService.addToNumPoints(1.0);

        //check in the cache
        int fileId = ev.getPacket().getPacketId();
        if(cache.get(fileId).getFileId() > 0) {
            cacheHit.addToNumPoints(1.0);
            cacheHit.addToCumulatedValue(1.0);

            double d = currentTime + ( fileMap.get(fileId).getSize() / institutionBandwidth);
            Event fileReceivedEvent = new Event(0, d, ev.getPacket(), fileId, EventType.FILE_RECEIVED_EVENT);

            cache.put(fileId, new FileMetadata(fileMap.get(fileId).getSize(),
                    fileMap.get(fileId).getPopularity(),
                    fileMap.get(fileId).getProbability()));
            eventPQ.enqueue(fileReceivedEvent);
        }
        else {
            cacheMiss.addToNumPoints(1.0);
            cacheMiss.addToCumulatedValue(1.0);

            double d = currentTime + logNormalDist.sample();
            Event cacheMissEvent = new Event(0, d, ev.getPacket(), fileId, EventType.ARRIVE_AT_QUEUE_EVENT);
            eventPQ.enqueue(cacheMissEvent);
        }

        if(numRequests < totalRequests) {
            double nextRequestTime = currentTime + expDist.sample();
            int randomFileId = fileSelectDistribution.sample();
            Packet newPacket = new Packet(randomFileId,
                    fileMap.get(randomFileId).getSize(),
                    fileMap.get(randomFileId).getPopularity(),
                    fileMap.get(randomFileId).getProbability(),
                    nextRequestTime, nextRequestTime);
            Event newRequestEvent = new Event(0, nextRequestTime, newPacket, randomFileId, EventType.NEW_REQUEST_EVENT);
            eventPQ.enqueue(newRequestEvent);
        }
    }

    public static void fileReceivedEvent(Event ev) {
        responseTime.addToNumPoints(1.0);
        responseTime.addToCumulatedValue(currentTime - ev.getPacket().getRequestCreationTime());
        numFinished++;
    }

    public static void arriveAtQueueEvent(Event ev) {
        numInService--;
        queued.addToCumulatedValue(packetQueue.getTotalItems());
        queued.addToNumPoints(1.0);

        Packet packet = ev.getPacket();
        int fileId = packet.getPacketId();

        packet.setArriveAtQueueTime(currentTime);
        packetQueue.enqueue(packet);

        if(packetQueue.getTotalItems() == 1) {
            double d = currentTime + (packet.metadata().getSize() / fifoBandWidth);
            Event departQueueEvent = new Event(0, d, packet, fileId, EventType.DEPART_QUEUE_EVENT);
            eventPQ.enqueue(departQueueEvent);
        }
    }

    public static void departQueueEvent(Event ev) throws Exception {
        int fileId = ev.getPacket().getPacketId();
        cache.put(fileId, new FileMetadata(ev.getPacket().metadata().getSize(),
                    ev.getPacket().metadata().getPopularity(),
                    ev.getPacket().metadata().getProbability()));
        double d = currentTime + (ev.getPacket().metadata().getSize() / institutionBandwidth);
        queueingDelay.addToCumulatedValue(d - ev.getPacket().getArriveAtQueueTime());
        queueingDelay.addToNumPoints(1.0);
        Event fileReceivedEvent = new Event(0, d, ev.getPacket(), fileId, EventType.FILE_RECEIVED_EVENT);
        eventPQ.enqueue(fileReceivedEvent);
        packetQueue.dequeue(ev.getPacket());

        if(packetQueue.getTotalItems() > 0) {
            Packet headPacket = packetQueue.getHead();
            int newFileId = headPacket.getPacketId();
            double nextDepartTime = currentTime + (headPacket.metadata().getSize() / fifoBandWidth);
            Event departQueueEvent = new Event(0, nextDepartTime, headPacket, newFileId, EventType.DEPART_QUEUE_EVENT);
            eventPQ.enqueue(departQueueEvent);
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

        if (cacheMiss.getNumPoints() != 0.0 && cacheHit.getNumPoints() != 0.0) {
            mean = cacheHit.getCumulatedValue() / (cacheHit.getNumPoints() + cacheMiss.getNumPoints());
            System.out.println("Cache hit ratio: " + mean);
            mean = cacheMiss.getCumulatedValue() / (cacheHit.getNumPoints() + cacheMiss.getNumPoints());
            System.out.println("Cache miss ratio: " + mean);
        }

    }

    public static void panic(String s) {
        System.out.println("****** Simulator Panic ******");
        System.out.println("   Message : " + s);
        System.exit(1);
    }
}
