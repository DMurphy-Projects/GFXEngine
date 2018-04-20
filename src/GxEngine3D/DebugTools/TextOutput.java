package GxEngine3D.DebugTools;

public class TextOutput {
    private static boolean debug = false;

    public static void setDebug(boolean b)
    {
        debug = b;
    }

    public static void println(String s)
    {
        if (debug)
        {
            System.out.println(Thread.currentThread().getStackTrace()[2].getClassName() + " - " + s);
        }
    }
}
