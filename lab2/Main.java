public class Main {
    public static class Semafor{
        private Boolean isInUse = false;

        public synchronized void P(){
            if(!isInUse)
                this.isInUse = true;
            else {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public synchronized void V() {
            if(isInUse){
                this.isInUse = false;
                notify();
            }
        }
    }

    public static class Counter implements Runnable{
        private Semafor s;
        private Integer cnt;
        private Boolean increment;

        public Counter(Semafor s, Integer cnt, Boolean increment){
            this.s = s;
            this.cnt = cnt;
            this.increment = increment;
        }


        @Override
        public void run() {
            for (int i = 0; i < 1000000; ++i) {
                this.s.P();
                if (increment) {
                    cnt++;
                } else {
                    cnt--;
                }
                this.s.V();
            }
        }
    }

    public static void main(String[] args){
        Semafor s = new Semafor();
        Integer cnt = 0;

        Thread thread1 = new Thread(new Counter(s, cnt, true));
        Thread thread2 = new Thread(new Counter(s, cnt, false));

        thread1.start();
        thread2.start();
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println(cnt);
    }
}
