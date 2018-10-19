package com.company;

import static java.lang.Thread.sleep;

public class ThreadPrinter {

    public static class NumberPrinter{
        private int threadIdToRun = 1;

        public synchronized void printTheNumber(Integer threadId){
                if (threadId != threadIdToRun) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.print(threadId + " ");

                    threadIdToRun = 1 + threadId % 3;
                    notifyAll();
                }
        }
    }

    static class Printer implements Runnable {

        private int threadId;
        NumberPrinter numberPrinter;

        public Printer(int threadId, NumberPrinter numberPrinter) {
            this.threadId = threadId;
            this.numberPrinter = numberPrinter;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    sleep(100);
                    numberPrinter.printTheNumber(this.threadId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public static void main(String[] args){
        NumberPrinter numberPrinter = new NumberPrinter();
        Thread t1 = new Thread(new Printer(1, numberPrinter));
        Thread t2 = new Thread(new Printer(2, numberPrinter));
        Thread t3 = new Thread(new Printer(3, numberPrinter));

        t3.start();
        t2.start();
        t1.start();
    }
}
