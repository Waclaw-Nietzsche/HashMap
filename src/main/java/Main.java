import Interfaces.Map;
import Realizations.CoarseHashMap;
import Realizations.RefinableCuckooHashMap;
import Realizations.RefinableHashMap;
import Realizations.StripedCuckooHashMap;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class Main
{
    final static int keyLimitValue = 1000000;

    // Добавление потоком в таблицу числа
    public static class putThread extends Thread
    {
        int iterations;
        Map<Integer, Integer> hashmap;

        putThread(int _iterations, Map<Integer, Integer> _hashmap)
        {
            this.iterations = _iterations;
            this.hashmap = _hashmap;
        }

        public void run()
        {
            for (int i = 0; i < iterations; i++)
            {
                // Генерируем случайные значения от 0 до предела Integer
                hashmap.put(ThreadLocalRandom.current().nextInt(0, keyLimitValue), ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE));
            }
        }
    }

    // Удаление потоком числа из таблицы
    public static class removeThread extends Thread
    {
        int iterations;
        Map<Integer, Integer> hashmap;

        removeThread(int _iterations, Map<Integer, Integer> _hashmap)
        {
            this.iterations = _iterations;
            this.hashmap = _hashmap;
        }

        public void run()
        {
            for (int i = 0; i < iterations; i++)
            {
                hashmap.remove(ThreadLocalRandom.current().nextInt(0, keyLimitValue));
            }
        }
    }

    // Проверка потоком наличия числа
    public static class containsThread extends Thread
    {
        int iterations;
        Map<Integer, Integer> hashmap;

        containsThread(int _iterations, Map<Integer, Integer> _hashmap)
        {
            this.iterations = _iterations;
            this.hashmap = _hashmap;
        }

        public void run()
        {
            for (int i = 0; i < iterations; i++)
            {
                hashmap.containsKey(ThreadLocalRandom.current().nextInt(0, keyLimitValue));
            }
        }
    }

    // Получение потоком числа из таблицы
    public static class getThread extends Thread
    {
        int iterations;
        Map<Integer, Integer> hashmap;

        getThread(int _iterations, Map<Integer, Integer> _hashmap)
        {
            this.iterations = _iterations;
            this.hashmap = _hashmap;
        }

        public void run()
        {
            for (int i = 0; i < iterations; i++)
            {
                hashmap.get(ThreadLocalRandom.current().nextInt(0, keyLimitValue));
            }
        }
    }


    public static void main(String[] args) throws InterruptedException
    {
        // Настройка параметров
        int runsOfAlgorithm = 5;

        int hashmapSize = 1000;

        int amountOfAddThreadOperations = 100;
        int amountOfGetThreadOperations = 100;
        int amountOfRemoveThreadOperations = 100;
        int amountOfContainsThreadOperations = 100;

        int amountOfAddThreadOperationsRepeat = 1000;
        int amountOfGetThreadOperationsRepeat = 1000;
        int amountOfRemoveThreadOperationsRepeat = 1000;
        int amountOfContainsThreadOperationsRepeat = 1000;

        ArrayList<Map<Integer, Integer>> hashmapList = new ArrayList<Map<Integer, Integer>>();

        hashmapList.add(new CoarseHashMap<>(hashmapSize));
        hashmapList.add(new RefinableHashMap<>(hashmapSize));
        hashmapList.add(new StripedCuckooHashMap<Integer, Integer>(hashmapSize));
        hashmapList.add(new RefinableCuckooHashMap<Integer, Integer>(hashmapSize));

        for (int currentHashMapIndex = 0; currentHashMapIndex < hashmapList.size(); currentHashMapIndex += 1)
        {
            long totalTime = 0;
            for (int currentRun = 0; currentRun < runsOfAlgorithm; currentRun += 1)
            {
                ArrayList<Thread> threadList = new ArrayList<Thread>();
                Thread currentThread;

                long startTime = System.nanoTime();

                // Put
                for (int i = 0; i < amountOfAddThreadOperations; i++)
                {
                    currentThread = new putThread(amountOfAddThreadOperationsRepeat, hashmapList.get(currentHashMapIndex));
                    threadList.add(currentThread);
                    currentThread.start();
                }
                // Get
                for (int i = 0; i < amountOfGetThreadOperations; i++)
                {
                    currentThread = new getThread(amountOfGetThreadOperationsRepeat, hashmapList.get(currentHashMapIndex));
                    threadList.add(currentThread);
                    currentThread.start();
                }

                // Contains
                for (int i = 0; i < amountOfContainsThreadOperations; i++)
                {
                    currentThread = new containsThread(amountOfContainsThreadOperationsRepeat, hashmapList.get(currentHashMapIndex));
                    threadList.add(currentThread);
                    currentThread.start();
                }

                // remove
                for (int i = 0; i < amountOfRemoveThreadOperations; i++)
                {
                    currentThread = new removeThread(amountOfRemoveThreadOperationsRepeat, hashmapList.get(currentHashMapIndex));
                    threadList.add(currentThread);
                    currentThread.start();
                }

                // Синхронизация
                for (Thread t : threadList)
                {
                    t.join();
                }

                // Вычисление времени работы
                long estimatedTime = System.nanoTime() - startTime;
                totalTime += estimatedTime;
            }


            // Отображаем текущий алгоритм хеш-таблицы
            switch (currentHashMapIndex)
            {
                case 0:
                    System.out.println("Текущий алгоритм: CoarseHashMap");
                    break;
                case 1:
                    System.out.println("Текущий алгоритм: RefinableHashMap");
                    break;
                case 2:
                    System.out.println("Текущий алгоритм: StripedCuckooHashMap");
                    break;
                case 3:
                    System.out.println("Текущий алгоритм: RefinableCuckooHashMap");
                    break;
            }

            // Общее время работы
            System.out.println("Времени затрачено: " + (double) totalTime / runsOfAlgorithm / 1000000 + " Миллисекунд.");
        }
    }
}
