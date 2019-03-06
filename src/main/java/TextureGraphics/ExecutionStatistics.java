package TextureGraphics;

import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_event;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static GxEngine3D.Helper.PerformanceTimer.Mode.Nano;

//source from http://www.jocl.org/samples/JOCLEventSample.java
//adapted to include different formats
public class ExecutionStatistics
{
    public enum Formats
    {
        Nano, Milli
    }
    /**
     * A single entry of the ExecutionStatistics
     */
    private static class Entry
    {
        private String name;
        private long submitTime[] = new long[1];
        private long queuedTime[] = new long[1];
        private long startTime[] = new long[1];
        private long endTime[] = new long[1];

        String format, suffix;
        double divisor;
        DecimalFormat dFormat;

        Entry(String name, cl_event event, Formats f)
        {
            this.name = name;
            CL.clGetEventProfilingInfo(
                    event, CL.CL_PROFILING_COMMAND_QUEUED,
                    Sizeof.cl_ulong, Pointer.to(queuedTime), null);
            CL.clGetEventProfilingInfo(
                    event, CL.CL_PROFILING_COMMAND_SUBMIT,
                    Sizeof.cl_ulong, Pointer.to(submitTime), null);
            CL.clGetEventProfilingInfo(
                    event, CL.CL_PROFILING_COMMAND_START,
                    Sizeof.cl_ulong, Pointer.to(startTime), null);
            CL.clGetEventProfilingInfo(
                    event, CL.CL_PROFILING_COMMAND_END,
                    Sizeof.cl_ulong, Pointer.to(endTime), null);

            switch (f)
            {
                case Nano: nanoSecondTime(); break;
                case Milli: milliSecondTime(); break;
            }
        }

        void milliSecondTime()
        {
            format = "%8.3f";
            suffix = "ms";
            divisor = 1e6;
        }

        void nanoSecondTime()
        {
            format = "%s";
            suffix = "ns";
            divisor = 1;
        }

        void normalize(long baseTime)
        {
            submitTime[0] -= baseTime;
            queuedTime[0] -= baseTime;
            startTime[0] -= baseTime;
            endTime[0] -= baseTime;
        }

        long getQueuedTime()
        {
            return queuedTime[0];
        }

        void print()
        {
            System.out.println("Event "+name+": ");
            System.out.println("Queued : "+
                    String.format(format, queuedTime[0]/divisor)+" "+suffix);
            System.out.println("Submit : "+
                    String.format(format, submitTime[0]/divisor)+" "+suffix);
            System.out.println("Start  : "+
                    String.format(format, startTime[0]/divisor)+" "+suffix);
            System.out.println("End    : "+
                    String.format(format, endTime[0]/divisor)+" "+suffix);

            long duration = endTime[0]-startTime[0];
            System.out.println("Time   : "+
                    String.format(format, duration / divisor)+" "+suffix);
        }
    }

    /**
     * The list of entries in this instance
     */
    private List<Entry> entries = new ArrayList<Entry>();

    /**
     * Adds the specified entry to this instance
     *
     * @param name A name for the event
     * @param event The event
     */
    public void addEntry(String name, cl_event event, Formats format)
    {
        entries.add(new Entry(name, event, format));
    }

    /**
     * Removes all entries
     */
    public void clear()
    {
        entries.clear();
    }

    /**
     * Normalize the entries, so that the times are relative
     * to the time when the first event was queued
     */
    private void normalize()
    {
        long minQueuedTime = Long.MAX_VALUE;
        for (Entry entry : entries)
        {
            minQueuedTime = Math.min(minQueuedTime, entry.getQueuedTime());
        }
        for (Entry entry : entries)
        {
            entry.normalize(minQueuedTime);
        }
    }

    /**
     * Print the statistics
     */
    public void print()
    {
        normalize();
        for (Entry entry : entries)
        {
            entry.print();
        }
    }

    public void printTotal()
    {
        normalize();
        long totalTime = 0;
        long totalWaiting = 0;
        for (Entry entry : entries)
        {
            totalTime += entry.endTime[0] - entry.startTime[0];
            totalWaiting += entry.startTime[0] - entry.submitTime[0];
        }

        Entry e = entries.get(0);
        System.out.println();
        System.out.println("Kernel Time : "+
                String.format(e.format, totalTime/e.divisor)+" "+e.suffix);

        System.out.println("Total Waiting : "+
                String.format(e.format, totalWaiting/e.divisor)+" "+e.suffix);
    }
}