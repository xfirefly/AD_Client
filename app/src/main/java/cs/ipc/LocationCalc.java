package cs.ipc;

public class LocationCalc {

    public static int ToScreenX(float i, float canvasWidth) {
        int n = (int) (1920 * i / canvasWidth);
        //Console.WriteLine("xx " + n + " " + (1920 * i / canvasWidth));
        return n;
    }

    public static float ToFormX(float i, float canvasWidth) {
        return (canvasWidth * i / 1920);
    }

    public static int ToScreenY(float i, float canvasHeight) {
        int n = (int) (1080 * i / canvasHeight);
        //Console.WriteLine("yy " + n + " " + (1080 * i / canvasHeight));
        return n;
    }

    public static float ToFormY(float i, float canvasHeight) {
        return (canvasHeight * i / 1080);
    }

}