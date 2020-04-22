import Interfaces.Map;
import Realizations.*;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class Main {
    private final static int KEY_LIMIT_VALUE = 100000;
    private final static int HASH_MAP_SIZE = 1000;
    private final static int LOAD_FACTOR = 1;
    private final static int MAX_THREADS_COUNT = 32;
    private final static int THREAD_MULTIPLY_VALUE = 2;
    private final static int OPERATIONS_PER_THREAD = 100000;
    private final static int ADD_OPERATIONS_COUNT = (int) (0.25F * OPERATIONS_PER_THREAD);
    private final static int GET_OPERATIONS_COUNT = (int) (0.25F * OPERATIONS_PER_THREAD);
    private final static int REMOVE_OPERATIONS_COUNT = (int) (0.25F * OPERATIONS_PER_THREAD);
    private final static int CONTAINS_OPERATIONS_COUNT = (int) (0.25F * OPERATIONS_PER_THREAD);

    public static void main(String[] args) throws InterruptedException {

        ArrayList<Map<Integer, Integer>> hashmapList = new ArrayList<>();

        StringBuilder result = new StringBuilder("Threads count,CoarseHashMap,RefinableHashMap,StripedCuckooHashmap,RefinableCuckooHashMap,ConcurrentHopscotchHashMap");
        for (int currentThreadCount = 1; currentThreadCount <= MAX_THREADS_COUNT; currentThreadCount *= THREAD_MULTIPLY_VALUE) {
            hashmapList.clear();

            hashmapList.add(new CoarseHashMap<>(HASH_MAP_SIZE));
            hashmapList.add(new RefinableHashMap<>(HASH_MAP_SIZE));
            hashmapList.add(new StripedCuckooHashMap<>(HASH_MAP_SIZE));
            hashmapList.add(new RefinableCuckooHashMap<>(HASH_MAP_SIZE));
            hashmapList.add(new ConcurrentHopscotchHashMap<>(HASH_MAP_SIZE, currentThreadCount));
            result.append("\n").append(currentThreadCount).append(",");
            for (Map<Integer, Integer> hashMap : hashmapList) {
                //Initial data
                generateInitialData(hashMap);

                //Threads
                ArrayList<Thread> threadList = new ArrayList<>();
                Thread currentThread;

                long startTime = System.nanoTime();

                for (int i = 0; i < currentThreadCount; i++) {
                    currentThread = new OperationsThread(hashMap);
                    threadList.add(currentThread);
                    currentThread.start();
                }

                // Sync
                for (Thread t : threadList) {
                    t.join();
                }

                // Time measurement
                double totalTimeMilliseconds = (System.nanoTime() - startTime) / 1000000.0;
                // Time taken
                result.append((OPERATIONS_PER_THREAD * currentThreadCount) / totalTimeMilliseconds).append(",");
                //result.append(totalTimeMilliseconds / currentThreadCount).append(",");
            }
        }
        System.out.print(result.toString());
    }

    private static void generateInitialData(Map<Integer, Integer> hashMap) {
        for (int i = 0; i < HASH_MAP_SIZE * LOAD_FACTOR; i++) {
            hashMap.put(ThreadLocalRandom.current().nextInt(0, KEY_LIMIT_VALUE), 2 * HASH_MAP_SIZE * LOAD_FACTOR);
        }
    }

    public static class OperationsThread extends Thread {
        private final Map<Integer, Integer> hashMap;

        OperationsThread(Map<Integer, Integer> hashMap) {
            this.hashMap = hashMap;
        }

        @Override
        public void run() {
            //Put
            for (int i = 0; i < ADD_OPERATIONS_COUNT; i++) {
                hashMap.put(ThreadLocalRandom.current().nextInt(0, KEY_LIMIT_VALUE), ThreadLocalRandom.current().nextInt(0, 2 * LOAD_FACTOR * HASH_MAP_SIZE));
            }
            //Contains
            for (int i = 0; i < CONTAINS_OPERATIONS_COUNT; i++) {
                hashMap.containsKey(ThreadLocalRandom.current().nextInt(0, KEY_LIMIT_VALUE));
            }
            //Remove
            for (int i = 0; i < REMOVE_OPERATIONS_COUNT; i++) {
                hashMap.remove(ThreadLocalRandom.current().nextInt(0, KEY_LIMIT_VALUE));
            }
            //Get
            for (int i = 0; i < GET_OPERATIONS_COUNT; i++) {
                hashMap.get(ThreadLocalRandom.current().nextInt(0, KEY_LIMIT_VALUE));
            }
        }
    }

    // Добавление потоком в таблицу числа
    public static class PutThread extends Thread {
        int iterations;
        Map<Integer, Integer> hashmap;

        PutThread(int _iterations, Map<Integer, Integer> _hashmap) {
            this.iterations = _iterations;
            this.hashmap = _hashmap;
        }

        public void run() {
            for (int i = 0; i < iterations; i++) {
                // Генерируем случайные значения от 0 до предела Integer
                hashmap.put(ThreadLocalRandom.current().nextInt(0, KEY_LIMIT_VALUE), ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE));
            }
        }
    }
}
