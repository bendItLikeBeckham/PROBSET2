import java.io.*;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//Syncrhonization Mechanism used to control access to shared resources
import java.util.concurrent.Semaphore;

class GameInstance {
    
    private int id;
    private boolean active;
    private static int totalPartiesServed = 0;
    private static int totalTimeServed = 0;


    private static final Random random = new Random();
    private static final Object lock = new Object();

    public GameInstance(int id) {
        this.id = id;
        this.active = false;
    }

    public synchronized void runInstance(int min, int max) {
        int finishTime = random.nextInt((max - min) + 1) + min;
        this.active = true;
        System.out.println("Instance " + id + " is active. The set completion time is " + finishTime + " seconds.");
        
        try {
            Thread.sleep(finishTime * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Instance " + id + " has completed. Now empty.");
        this.active = false;
        
        synchronized (lock) {
            totalPartiesServed++;
            totalTimeServed += finishTime;
        }
    }

    public static void printSummary() {
        System.out.println("\nSummary:");
        System.out.println("Total parties served: " + totalPartiesServed);
        System.out.println("Total time served: " + totalTimeServed + " seconds");   
    }
}

public class LFG {
    public static void main(String[] args) {
        Properties properties = new Properties();
        
        try (FileInputStream config = new FileInputStream("config.properties")) {
            properties.load(config);

            int n = Integer.parseInt(properties.getProperty("n"));
            int t = Integer.parseInt(properties.getProperty("t"));
            int h = Integer.parseInt(properties.getProperty("h"));
            int d = Integer.parseInt(properties.getProperty("d"));
            int t1 = Integer.parseInt(properties.getProperty("t1"));
            int t2 = Integer.parseInt(properties.getProperty("t2"));

            if (n <= 0 || t < 0 || h < 0 || d < 0 || t1 <= 0 || t2 <= 0) {
                System.out.println("Error: All values must be positive and greater than zero where applicable.");
                return;
            }

            if (t2 <= t1) {
                System.out.println("Error: Maximum clear time (t2) must be greater than minimum clear time (t1).");
                return;
            }

            int maxParties = Math.min(Math.min(t, h), d / 3);
            int excessTanks = t - maxParties;
            int excessHealers = h - maxParties;
            int excessDPS = d - (maxParties * 3);

            System.out.println("----------------------- Dungeon Finder -------------------------------------");
            System.out.println("There are " + maxParties + " parties that can be formed.");
            System.out.println("Only " + n + " parties can be active in the dungeon at a time.");
            System.out.println("\nExcess Tanks: " + Math.max(0, excessTanks));
            System.out.println("Excess Healers: " + Math.max(0, excessHealers));
            System.out.println("Excess DPS: " + Math.max(0, excessDPS));
            System.out.println("----------------------------------------------------------------------------");


            Semaphore semaphore = new Semaphore(n);
            ExecutorService executor = Executors.newFixedThreadPool(n);
            GameInstance[] instances = new GameInstance[n];
            
            for (int i = 0; i < n; i++) {
                instances[i] = new GameInstance(i + 1);
            }

            for (int i = 0; i < maxParties; i++) {
                try {
                    semaphore.acquire();
                    int instanceId = i % n;
                    executor.execute(() -> {
                        instances[instanceId].runInstance(t1, t2);
                        semaphore.release();
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            executor.shutdown();
            while (!executor.isTerminated()) {
            }

            GameInstance.printSummary();
        } catch (IOException | NumberFormatException e) {
            System.out.println("Error reading configuration file: " + e.getMessage());
        }
    }
}
