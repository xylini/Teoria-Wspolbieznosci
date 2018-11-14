import java.util.concurrent.Callable;

public class MandelbrotDrawer implements Callable<Long> {
    private int y_start;
    private int y_stop;
    private Mandelbrot mandelbrot;
    private Integer[][] iteration;

    public MandelbrotDrawer(int y_start, int y_stop, Mandelbrot mandelbrot, Integer[][] iteration){
        this.y_start = y_start;
        this.y_stop = y_stop;
        this.mandelbrot = mandelbrot;
        this.iteration = iteration;
    }



    @Override
    public Long call() {
        double zx, zy, cX, cY, tmp;
        for (int y = y_start; y < y_stop && y < mandelbrot.getHeight(); y++) {
            for (int x = 0; x < mandelbrot.getWidth(); x++) {
                zx = zy = 0;
                cX = (x - (double)mandelbrot.getWidth() / 2) / mandelbrot.getZOOM();
                cY = (y - (double)mandelbrot.getHeight() / 2) / mandelbrot.getZOOM();
                int iter = mandelbrot.getMAX_ITER();

                while (zx * zx + zy * zy < 4 && iter > 0) {
                    tmp = zx * zx - zy * zy + cX;
                    zy = 2.0 * zx * zy + cY;
                    zx = tmp;
                    iter--;
                }

                this.iteration[x][y] = iter;
            }
        }


        return System.nanoTime();
    }
}
