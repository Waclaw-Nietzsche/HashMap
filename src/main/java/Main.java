import Interfaces.Map;
import Realizations.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Main {
    private final static int KEY_LIMIT_VALUE = 1000000;
    private final static int HASH_MAP_SIZE = 1000;
    private final static int MAX_THREADS_COUNT = 16;
    private final static int THREAD_INCREMENT_VALUE = 2;
    private final static int OPERATIONS_PER_THREAD = 1000;

    public static void main(String[] args) throws InterruptedException {
        int amountOfAddThreadOperations = 100;
        int amountOfGetThreadOperations = 100;
        int amountOfRemoveThreadOperations = 100;
        int amountOfContainsThreadOperations = 100;

        ArrayList<Map<Integer, Integer>> hashmapList = new ArrayList<>();

        List<String> result = new ArrayList<>();

        for (int currentThreadCount = 1; currentThreadCount <= MAX_THREADS_COUNT; currentThreadCount *= THREAD_INCREMENT_VALUE) {
            hashmapList.clear();
            hashmapList.add(new CoarseHashMap<>(HASH_MAP_SIZE));
            hashmapList.add(new RefinableHashMap<>(HASH_MAP_SIZE));
            hashmapList.add(new StripedCuckooHashMap<>(HASH_MAP_SIZE));
            hashmapList.add(new RefinableCuckooHashMap<>(HASH_MAP_SIZE));
            hashmapList.add(new ConcurrentHopscotchHashMap<>(HASH_MAP_SIZE, currentThreadCount));

            for (Map<Integer, Integer> hashMap : hashmapList) {
                long totalTime = 0;
                ArrayList<Thread> threadList = new ArrayList<>();
                Thread currentThread;

                long startTime = System.nanoTime();

                // Put
                for (int i = 0; i < amountOfAddThreadOperations; i++) {
                    currentThread = new putThread(OPERATIONS_PER_THREAD, hashMap);
                    threadList.add(currentThread);
                    currentThread.start();
                }
                // Get
                for (int i = 0; i < amountOfGetThreadOperations; i++) {
                    currentThread = new getThread(OPERATIONS_PER_THREAD, hashMap);
                    threadList.add(currentThread);
                    currentThread.start();
                }

                // Contains
                for (int i = 0; i < amountOfContainsThreadOperations; i++) {
                    currentThread = new containsThread(OPERATIONS_PER_THREAD, hashMap);
                    threadList.add(currentThread);
                    currentThread.start();
                }

                // remove
                for (int i = 0; i < amountOfRemoveThreadOperations; i++) {
                    currentThread = new removeThread(OPERATIONS_PER_THREAD, hashMap);
                    threadList.add(currentThread);
                    currentThread.start();
                }

                // Синхронизация
                for (Thread t : threadList) {
                    t.join();
                }

                // Вычисление времени работы
                long estimatedTime = System.nanoTime() - startTime;
                totalTime += estimatedTime;

                // Общее время работы
                System.out.println(currentThreadCount + "," + hashMap + "," + (double) totalTime / currentThreadCount / 1000000 + " Миллисекунд.");
            }
        }
    }

    // Добавление потоком в таблицу числа
    public static class putThread extends Thread {
        int iterations;
        Map<Integer, Integer> hashmap;

        putThread(int _iterations, Map<Integer, Integer> _hashmap) {
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

    // Удаление потоком числа из таблицы
    public static class removeThread extends Thread {
        int iterations;
        Map<Integer, Integer> hashmap;

        removeThread(int _iterations, Map<Integer, Integer> _hashmap) {
            this.iterations = _iterations;
            this.hashmap = _hashmap;
        }

        public void run() {
            for (int i = 0; i < iterations; i++) {
                hashmap.remove(ThreadLocalRandom.current().nextInt(0, KEY_LIMIT_VALUE));
            }
        }
    }

    // Проверка потоком наличия числа
    public static class containsThread extends Thread {
        int iterations;
        Map<Integer, Integer> hashmap;

        containsThread(int _iterations, Map<Integer, Integer> _hashmap) {
            this.iterations = _iterations;
            this.hashmap = _hashmap;
        }

        public void run() {
            for (int i = 0; i < iterations; i++) {
                hashmap.containsKey(ThreadLocalRandom.current().nextInt(0, KEY_LIMIT_VALUE));
            }
        }
    }

    // Получение потоком числа из таблицы
    public static class getThread extends Thread {
        int iterations;
        Map<Integer, Integer> hashmap;

        getThread(int _iterations, Map<Integer, Integer> _hashmap) {
            this.iterations = _iterations;
            this.hashmap = _hashmap;
        }

        public void run() {
            for (int i = 0; i < iterations; i++) {
                hashmap.get(ThreadLocalRandom.current().nextInt(0, KEY_LIMIT_VALUE));
            }
        }
    }
}
