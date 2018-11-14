import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import javax.swing.JFrame;

public class MandelbrotCachedThreadExecutor extends JFrame {

    private final int MAX_ITER = 1000;
    private final double ZOOM = 300;
    private BufferedImage I;
    private double zx, zy, cX, cY, tmp;

    public MandelbrotCachedThreadExecutor(int width, int height) {
        super("Mandelbrot Set");

        setBounds(100, 100, width, height);
        setResizable(false);


        setDefaultCloseOperation(EXIT_ON_CLOSE);
        I = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);

    }

    public double getZOOM(){
        return this.ZOOM;
    }

    public void setI(int x, int y, int iter){
        I.setRGB(x, y, iter | iter << 8);
    }

    public int getMAX_ITER(){
        return MAX_ITER;
    }

    @Override
    public void paint(Graphics g) {
        g.drawImage(I, 0, 0, this);
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException, IOException {
        int width = 1000;
        int height = 900;
        Integer[][] iter_values = new Integer[width][height];
        PrintWriter printWriter = new PrintWriter(new FileWriter("./newCachedThreadPool.csv"));
        printWriter.println("implementation, tasks, threads, time");
        printWriter.flush();


        Mandelbrot mandelbrot = new Mandelbrot(width, height);
        for(int probe = 1; probe < 10; probe++){
            for(int threads_number = 1; threads_number <= 90; threads_number += 10) {
                ExecutorService executorService = Executors.newCachedThreadPool();
                ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executorService;

                int tasks_number = threads_number * 10;
                int y_start = 0;
                int y_step = height / tasks_number;

                List<Future<Long>> futureList = new ArrayList<>();

                Long start_time = System.nanoTime();
                for (int i = 0; i < tasks_number; i++) {
                    y_start = i * y_step;
                    Future future = executorService.submit(new MandelbrotDrawer(y_start, y_start + y_step, mandelbrot, iter_values));
                    futureList.add(future);
                }


                Long latest_time = Long.valueOf(0);
                for (Future<Long> future : futureList) {
                    if (latest_time < future.get()) {
                        latest_time = future.get();
                    }
                }

                Long accurate_time = latest_time - start_time;

                printWriter.flush();
                printWriter.println("newCachedThreadPool, " + tasks_number + ", " + threadPoolExecutor.getPoolSize() + ", " + accurate_time);
                System.out.println("Impl: newCachedThreadPool, Tasks: " + tasks_number + ", max threads: " + threadPoolExecutor.getPoolSize() + ", Time: " + accurate_time.toString());
            }
        }

        for(int i = 0; i < width; i++){
            for(int j = 0; j < height; j++){
                mandelbrot.setI(i, j, iter_values[i][j]);
            }
        }
        mandelbrot.setVisible(true);
    }
}