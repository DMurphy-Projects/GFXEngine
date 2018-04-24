package DebugTools;

import DebugTools.TextModule.BaseTextModule;
import DebugTools.TextModule.NullImplement;
//Note:
//categories are used as follows:
//0: default
//1: permanent debug but only occasional
//2: permanent debug and constant
public class TextOutput {

    private static BaseTextModule module;
    private static String lastClass = "";
    public enum Mode
    {
        MINIMAL,
        NORMAL,
        EXTENDED
    }
    private static Mode current;

    static
    {
        //the least amount of performance impact with this setup
        module = new NullImplement();
        current = Mode.NORMAL;
    }

    public static void setModule(BaseTextModule m)
    {
        module = m;
    }
    public static void setMode(Mode m) {
        current = m;
    }

    public static void println(Object o)
    {
        //assume a default category
        println(o, 0, 3);
    }
    public static void println(Object o, int cat)
    {
        println(o, cat, 3);
    }

    private static void println(Object o, int cat, int pos)
    {
        StackTraceElement e = Thread.currentThread().getStackTrace()[pos];
        String c = e.getClassName();
        String s = o.toString();
        if (module.allow(c, cat)) {
            if (current.equals(Mode.NORMAL))
            {
                System.out.println(c + " - " + s);
            }
            else if (current.equals(Mode.MINIMAL)) {
                if (!lastClass.equals(c))
                {
                    lastClass = c;
                    System.out.println(c);
                }
                System.out.println("    " + s);
            }
            else if (current.equals(Mode.EXTENDED))
            {
                System.out.println(String.format("%s in %s(line %s): %s", c, e.getMethodName(), e.getLineNumber(), s));
            }
        }
    }
}
