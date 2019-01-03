package GxEngine3D.Helper;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class PerformanceTimer {

    public enum Mode
    {
        Nano, Percentage, Total
    }

    ArrayList<Long> times = new ArrayList<>();
    int index = 0;

    Mode mode;

    DecimalFormat percentage = new DecimalFormat("#0.0#");
    DecimalFormat nano = new DecimalFormat("000, 000, 000");

    public PerformanceTimer(Mode m)
    {
        mode = m;
    }

    public void time()
    {
        times.add(System.nanoTime());
    }

    public void printNextTime(String s)
    {
        long timeTaken = times.get(index+1) - times.get(index);
        double total = times.get(times.size()-1) - times.get(0);

        switch (mode)
        {
            case Nano:
                System.out.println(String.format("%s : %sns", s, nano.format(timeTaken)));
                break;
            case Percentage:
                System.out.println(String.format("%s : %s%%", s, percentage.format(100 * timeTaken / total)));
                break;
            case Total:
                System.out.println(String.format("%s : %sns", s, nano.format(total)));
                break;
        }

        index++;
    }

    public void reset()
    {
        index = 0;
        times.clear();
    }
}
