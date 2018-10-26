import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ProducerConsumerFairSmallValues {

    public static class BoundedBuffer{
        final Lock lock = new ReentrantLock();
        final Condition PIERWSZYPROD = lock.newCondition();
        final Condition RESZTAPROD = lock.newCondition();
        final Condition PIERWSZYKONS = lock.newCondition();
        final Condition RESZTAKONS = lock.newCondition();
        AtomicBoolean isPIERWSZYPRODbusy = new AtomicBoolean(false);
        AtomicBoolean isPIERWSZYKONSbusy = new AtomicBoolean(false);
        private String elements = "";
        static Random generator = new Random();


        final Object[] items;
        int bufferSize;

        public BoundedBuffer(int bufferSize){
            this.items = new Object[bufferSize];
            this.bufferSize = bufferSize;
        }

        int putptr, takeptr, count;

        public void put(int elementsToProduce) throws InterruptedException{
            lock.lock();
            try{
                long tStart = System.nanoTime();
                if(isPIERWSZYPRODbusy.get()){
                    RESZTAPROD.await();
                }
                isPIERWSZYPRODbusy.set(true);


                while(this.bufferSize - count < elementsToProduce){
                    PIERWSZYPROD.await();
                }
                long tStop = System.nanoTime() - tStart;
                int bucket = elementsToProduce - elementsToProduce%(M/10) + M/10;
                producer_PrintWriter.flush();
                producer_PrintWriter.print(PK + ", " + M + ", " + bucket + ", ");
                producer_PrintWriter.println(tStop);

                for (int i = 0; i < elementsToProduce; i++) {
                    int elementToPut = generator.nextInt(100)+1;

                    items[putptr] = elementToPut;
                    if (++putptr == items.length) putptr = 0;
                    ++count;
                }

                RESZTAPROD.signal();
                isPIERWSZYKONSbusy.set(false);
                PIERWSZYKONS.signal();

            } finally {
                System.out.flush();
                System.out.println("Producer: " + ProducerDone.incrementAndGet());
                lock.unlock();
            }
        }

        public String take(int elementsToConsume) throws InterruptedException {
            lock.lock();
            try {
                long tStart = System.nanoTime();
                if(isPIERWSZYKONSbusy.get()){
                    RESZTAKONS.await();
                }

                isPIERWSZYKONSbusy.set(true);

                while (count < elementsToConsume){
                    PIERWSZYKONS.await();
                }
                long tStop = System.nanoTime() - tStart;

                int bucket = elementsToConsume - elementsToConsume%(M/10) + M/10;
                consumer_PrintWriter.flush();
                consumer_PrintWriter.print(PK + ", " + M + ", " + bucket + ", ");
                consumer_PrintWriter.println(tStop);

                this.elements = "";
                for (int i = 0; i < elementsToConsume; i++) {
                    elements = elements.concat(items[takeptr] + " ");
                    items[takeptr] = -1;
                    if (++takeptr == items.length) takeptr = 0;
                    --count;
                }

                RESZTAKONS.signal();
                isPIERWSZYPRODbusy.set(false);
                PIERWSZYPROD.signal();

            } finally {
                System.out.flush();
                System.out.println("Consumer: " + ConsumerDone.incrementAndGet());
                lock.unlock();
                return this.elements;
            }
        }
    }
    public static class Producer implements Runnable{
        private int producerId;
        private int maxProduction;
        BoundedBuffer boundedBuffer;
        static Random generator = new Random();

        public Producer(int producerId, int maxProduction, BoundedBuffer boundedBuffer){
            this.producerId = producerId;
            this.maxProduction = maxProduction;
            this.boundedBuffer = boundedBuffer;
        }


        @Override
        public void run() {

            while(true){
                try {
                    int smallOrNormal = generator.nextInt(10)+1;
                    int elementsToProduce = smallOrNormal > 3 ? generator.nextInt(maxProduction/100) : generator.nextInt(maxProduction);
                    boundedBuffer.put(elementsToProduce);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }
            }
        }
    }

    public static class Consumer implements Runnable{
        private int maxConsumption;
        BoundedBuffer boundedBuffer;
        private int consumerId;
        static Random generator = new Random();

        public Consumer(int consumerId, int maxConsumption, BoundedBuffer boundedBuffer){
            this.maxConsumption = maxConsumption;
            this.boundedBuffer = boundedBuffer;
            this.consumerId = consumerId;
        }


        @Override
        public void run() {
            while(true){
                try {
                    int smallOrNormal = generator.nextInt(10)+1;
                    int elementsToConsume = smallOrNormal > 3 ? generator.nextInt(maxConsumption/100) : generator.nextInt(maxConsumption);
                    String string = boundedBuffer.take(elementsToConsume);
                    //System.err.println("Consumer: " + string);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }
            }
        }

    }


    /* zapis do pliku */
    public static FileWriter producer_fileWriter;
    public static FileWriter consumer_fileWriter;
    public static int PK = 1;
    public static int M = 100;
    public static BoundedBuffer boundedBuffer;

    public static AtomicInteger ConsumerDone = new AtomicInteger(0);
    public static AtomicInteger ProducerDone = new AtomicInteger(0);

    static {
        try {
            producer_fileWriter = new FileWriter("/Users/jakub/IdeaProjects/TW-lab3/fairSVProducerPK1000M100000.csv");
            consumer_fileWriter = new FileWriter("/Users/jakub/IdeaProjects/TW-lab3/fairSVConsumerPK1000M100000.csv");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static PrintWriter producer_PrintWriter = new PrintWriter(producer_fileWriter);
    public static PrintWriter consumer_PrintWriter = new PrintWriter(consumer_fileWriter);
    //////////////////////

    public static void main(String[] args){
        producer_PrintWriter.println("producers, M, length, time");
        consumer_PrintWriter.println("consumer, M, length, time");
        producer_PrintWriter.flush();
        consumer_PrintWriter.flush();
        Thread cons_pool[];
        Thread prod_pool[];

        PK = 1000;
        M = 100000;

        Producer[] producers10 = new Producer[PK];
        Consumer[] consumers10 = new Consumer[PK];
        cons_pool = new Thread[PK];
        prod_pool = new Thread[PK];
        boundedBuffer = new BoundedBuffer(M * 2);

        for (int i = 0; i < PK; i++) {
            producers10[i] = new Producer(i, M, boundedBuffer);
            consumers10[i] = new Consumer(i, M, boundedBuffer);

            prod_pool[i] = new Thread(producers10[i]);
            cons_pool[i] = new Thread(consumers10[i]);
            prod_pool[i].start();
            cons_pool[i].start();
        }
    }
}

