import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.ParetoDistribution;
import org.apache.commons.math3.random.RandomGeneratorFactory;

import java.util.Arrays;
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
    // minimum of a Pareto random variable representing the workload size;
    // it is computed based on the input parameter Pareto_Alpha and WorkloadMean
    static double pareto_K;
    static EventPriorityQueue eventPQ;
    static FIFOQueue packetQueue;

    // the following are used to compute the mean
    static CumulativeMeasurement inService = new CumulativeMeasurement(0.0, 0.0);  // cumulative number in service
    static CumulativeMeasurement queued = new CumulativeMeasurement(0.0, 0.0); // cumulative number in the queue
    static CumulativeMeasurement queueingDelay = new CumulativeMeasurement(0.0, 0.0); // cumulative queueing in the queue
    static CumulativeMeasurement responseTime = new CumulativeMeasurement(0.0, 0.0); // cumulative response time

    static Distribution workLoadDistribution;
    static Random rand = new Random(randomSeed);
    static RandomGenerator rng = RandomGeneratorFactory.createRandomGenerator(rand);



    public static void main(String[] args) throws Exception {
        if( args.length != 2 ) {
            System.out.println(Arrays.toString(args));
            System.out.println( "Usage : javac Driver.java input_file seed_number");
            panic("main() : You'd better learn how to use this program");
        }

        eventPQ = EventPriorityQueue.getInstance();               // event priority queue
        packetQueue = FIFOQueue.getInstance();              // FIFO packet queue


        InputReader inp = new InputReader(args[0]);
        totalRequests = inp.getTotalRequests();
        totalTime = inp.getTotalTime();
        serverRate = inp.getServerRate();
        requestRate = inp.getRequestRate();

        workLoadMean = inp.getWorkLoadMean();
        paretoAlpha = inp.getParetoAlpha();
        randomSeed = Long.parseLong(args[1]);
        workLoadDistribution = inp.getWorkLoadDistribution();
        switch(workLoadDistribution) {
            case PARETO:
                pareto_K = (paretoAlpha-1.0)/paretoAlpha * workLoadMean;
                break;
            default:
                break;
        }
        System.out.println("Workload Mean: " + workLoadMean);
        if (workLoadDistribution == Distribution.PARETO){
            System.out.println("Pareto Alpha: " + paretoAlpha);
            System.out.println("Pareto K: " + pareto_K);
        }
        System.out.println("Random seed is " + randomSeed);

        long timeNow = System.currentTimeMillis();
        System.out.println("Simulation started at: " + timeNow);
        double workLoad = getWorkLoad();
        mainLoop(workLoad);
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

    public static void mainLoop(double workLoad) throws Exception {
        Packet packet = new Packet(numRequests + 1, 0.0, 0.0, workLoad);
        Event event = new Event(0, currentTime, packet, packet.getPacketId(), EventType.REQUEST_ARRIVAL_EVENT);
        eventPQ.enqueue(event);

        while(numFinished < totalRequests || currentTime < totalTime) {
            if (!eventPQ.isEmpty()) {
                Event e = eventPQ.dequeue();
                currentTime = e.key;
                switch(e.func) {
                    case REQUEST_ARRIVAL_EVENT:
                        requestArrivalTime(e);
                        break;
                    case END_OF_TRANSMISSION_EVENT:
                        endOfTransmissionEvent(e);
                        break;
                    case END_OF_SERVICE_EVENT:
                        endOfServiceEvent(e);
                        break;
                }
            }
        }
    }

    public static void requestArrivalTime(Event ev) {
        Packet p = ev.getPacket();
        p.setRequestTime(currentTime);

        numRequests++;
        numInService++;
        inService.addToCumulatedValue(numInService);
        inService.addToNumPoints(1.0);

        // send the request immediately to a server

        // create an end_of_service event. We assume an infinite number of servers.
        // The service time is proportional to the workload or file size.
        // For the first stage, we effectively have an M/G/infinity queue.

        // reuse the event object
        ev.setKey(currentTime + p.getWorkLoad()/serverRate);
        ev.setFunc(EventType.END_OF_SERVICE_EVENT);
        eventPQ.enqueue(ev);

        /* schedule a new request_arrival_event */
        p.setPacketId(numRequests + 1);
        p.setWorkLoad(getWorkLoad());
        double d = currentTime + getRequestInterArrivalTime();
        Event newEvent = new Event(0, d, p, p.getPacketId(), EventType.REQUEST_ARRIVAL_EVENT);
        eventPQ.enqueue(newEvent);

    }

    /*
     *    end_of_service event: end of service by a server. Send the packet to
     *    the FIFO packet queue
     */
    public static void endOfServiceEvent(Event ev) {
        numInService--;
        queued.addToCumulatedValue(packetQueue.getTotalItems());
        queued.addToNumPoints(1.0);

        Packet p = ev.getPacket();
        p.setArrivalTimeToQueue(currentTime);
        packetQueue.enqueue(p);

        // if pack is the only packet in the queue, schedule
        // an end_of_transmission_event
        if(packetQueue.getTotalItems() == 1) {
            ev.setKey(currentTime + p.getWorkLoad());
            ev.setFunc(EventType.END_OF_TRANSMISSION_EVENT);
            eventPQ.enqueue(ev);
        }
    }

    /*
     *    end_of_transmission event: schedule the next end_of_transmission_event
     *    if the queue is not empty
     */
    public static void endOfTransmissionEvent(Event ev) throws Exception {
        Packet p = ev.getPacket();
        packetQueue.dequeue(p);

        // collect data
        numFinished++;
        double d = currentTime - p.getRequestTime();
        responseTime.addToCumulatedValue(d);
        responseTime.addToNumPoints(1.0);

        d = currentTime - p.getArrivalTimeToQueue();
        queueingDelay.addToCumulatedValue(d);
        queueingDelay.addToNumPoints(1.0);

        if(packetQueue.getTotalItems() != 0) {
                /* If the queue is not empty, schedule an end_of_transmission_event
       for the next packet. Re-use ev. */
            p = packetQueue.getHead();
            ev.setPacket(p);
            ev.setPacketId(p.getPacketId());
            ev.setFunc(EventType.END_OF_TRANSMISSION_EVENT);
            ev.setKey(currentTime + p.getWorkLoad());
            eventPQ.enqueue(ev);
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
