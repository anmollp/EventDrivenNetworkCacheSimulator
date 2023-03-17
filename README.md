# EventDrivenNetworkCacheSimulator
By simulating a network cache using an
event driven model where in the requested files follow
a Pareto distribution, with a poisson file requests dis-
tribution along with caching techniques such as LRU,
Least Popular and FIFO, we try to understand the use
of probability in an ever growing world of internet

## Required jars:
1. commons-math3-3.6.1.jar
2. commons-math3-3.6.1-tools.jar
3. commons-math3-3.6.1-tests.jar
## Usage:
Extract the zip file **Project_Anmol_Nandigam.zip**

## Files
1. CumulativeMeasurement.java
2. Driver.java
3. Event.java
4. EventPriorityQueue.java
5. FIFOQueue.java
6. input.txt
7. Packet.java
8. InputReader.java
9. FileSelection.java
10. FileMetadata.java
11. Node.java
12. Cache.java
13. LRU Cache.java
14. LPCache.java
15. FIFOCache.java

## How to compile
1. Place the jars in the same folder as java files
2. To compile run:
  > javac -cp ":/your/path/commons-math3-3.6.1.jar:/your/path/commons-math3-3.6.1-tools.jar:/your/path/commons-math3-3.6.1-tests.jar" Driver.java
3. To run:
  > java -cp ":/your/path/commons-math3-3.6.1.jar:/your/path/commons-math3-3.6.1-tools.jar:/your/path/commons-math3-3.6.1-tests.jar" Driver <input-file> <random-seed>
4. Here input file is **input.txt** as provided in the files
5. **Random seed** is a long.
  
## Version of java used:
`java version "1.8.0_271"`
  
`Java(TM) SE Runtime Environment (build 1.8.0_271-b09)`
  
`Java HotSpot(TM) 64-Bit Server VM (build 25.271-b09, mixed mode)`

  
