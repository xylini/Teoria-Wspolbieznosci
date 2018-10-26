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
import java.lang.reflect.Method;

public class ProducerConsumerSmallValues {

    public static class Buffer{
        private int bufferSize;
        private int elementsInQueue;
        Queue<Integer> buffer;

        public Buffer(int bufferSize){
            this.elementsInQueue = 0;
            this.bufferSize = bufferSize;
            this.buffer = new LinkedList<>();
        }

        public void add(Integer element){

        }



    }

    public static class BoundedBuffer {
        final Lock lock = new ReentrantLock();
        final Condition notFull  = lock.newCondition();
        final Condition notEmpty = lock.newCondition();
        static Random generator = new Random();
        final Object[] items;
        int bufferSize;

        public BoundedBuffer(int bufferSize){
            this.items = new Object[bufferSize];
            this.bufferSize = bufferSize;
        }

        int putptr, takeptr, count;

        public boolean put(int elementsToProduce) throws InterruptedException {
            lock.lock();
            try {
                System.out.println("asdasdasd");
                long tStart = System.nanoTime();
                while (bufferSize - count < elementsToProduce)
                    notFull.await();
                long tStop = System.nanoTime() - tStart;
                //Long result = new Long(tStop);

                /* zapis do pliku */
                int bucket = elementsToProduce - elementsToProduce%(M/10) + M/10;
                producer_PrintWriter.flush();
                producer_PrintWriter.print(PK + ", " + M + ", " + bucket + ", ");
                producer_PrintWriter.println(tStop);

                //System.out.println("Producer: " + result.toString() + "\t" + elementsToProduce);

                for (int i = 0; i < elementsToProduce; i++) {
                    int elementToPut = generator.nextInt(100)+1;

                    items[putptr] = elementToPut;
                    if (++putptr == items.length) putptr = 0;
                    ++count;
                }


                notEmpty.signal();
            } finally {
                lock.unlock();
                return true;
            }
        }

        public String take(int elementsToConsume) throws InterruptedException {
            lock.lock();
            try {
                System.out.println("123123123");
                long tStart = System.nanoTime();
                while (count < elementsToConsume)
                    notEmpty.await();
                long tStop = System.nanoTime() - tStart;
                //Long result = new Long(tStop);
                //System.out.println(tStop);
                //System.out.println("Consumer: time -" + result.toString() + ",\telements - " + elementsToConsume);
                /* zapis do pliku */
                int bucket = elementsToConsume - (elementsToConsume%(M/10)) + M/10;
                consumer_PrintWriter.flush();
                consumer_PrintWriter.print(PK + ", " + M + ", " + bucket + ", ");
                consumer_PrintWriter.println(tStop);


                String elements = "";
                for (int i = 0; i < elementsToConsume; i++) {
                    elements = elements.concat(items[takeptr] + " ");
                    items[takeptr] = -1;
                    if (++takeptr == items.length) takeptr = 0;
                    --count;
                }

                notFull.signal();
                return elements;
            } finally {
                lock.unlock();
            }
        }
    }

    public static class Producer implements Runnable{
        private int producerId;
        private int maxProduction;
        BoundedBuffer boundedBuffer;
        static Random generator = new Random();
        private volatile boolean shouldRun = true;

        public Producer(int producerId, int maxProduction, BoundedBuffer boundedBuffer){
            this.producerId = producerId;
            this.maxProduction = maxProduction;
            this.boundedBuffer = boundedBuffer;
        }

        public void exit(){
            Thread.currentThread().interrupt();
        }

        @Override
        public void run() {

            while(shouldRun){
                try {
                    int smallOrNormal = generator.nextInt(10)+1;
                    int elementsToProduce = smallOrNormal > 3 ? generator.nextInt(maxProduction/100) : generator.nextInt(maxProduction);
                    if(boundedBuffer.put(elementsToProduce));
                    //System.err.println("Producer: " + elementsToProduce);
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
        private volatile boolean shouldRun = true;

        public Consumer(int consumerId, int maxConsumption, BoundedBuffer boundedBuffer){
            this.maxConsumption = maxConsumption;
            this.boundedBuffer = boundedBuffer;
            this.consumerId = consumerId;
        }

        public void exit(){
            Thread.currentThread().interrupt();
        }

        @Override
        public void run() {
            while(shouldRun){
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

    static {
        try {
            producer_fileWriter = new FileWriter("/Users/jakub/IdeaProjects/TW-lab3/SVproducerPK1000M100000.csv");
            consumer_fileWriter = new FileWriter("/Users/jakub/IdeaProjects/TW-lab3/SVconsumerPK1000M100000.csv");
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
