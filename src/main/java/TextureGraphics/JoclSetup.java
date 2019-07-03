package TextureGraphics;

import GxEngine3D.Helper.JoclSetupHelper;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_device_id;
import org.jocl.cl_platform_id;

public class JoclSetup {

    private cl_context context;
    private cl_command_queue commandQueue;
    private boolean profiling;

    public  JoclSetup(boolean profiling)
    {
        this.profiling = profiling;
        setup();
    }

    private void setup()
    {
        cl_platform_id platform = JoclSetupHelper.createPlatform();
        cl_device_id device = JoclSetupHelper.createDevice(platform);
        context = JoclSetupHelper.createContext(device, platform);
        commandQueue = JoclSetupHelper.createCommandQueue(context, device, profiling);
    }

    public cl_context getContext() {
        return context;
    }

    public cl_command_queue getCommandQueue() {
        return commandQueue;
    }

    public boolean isProfiling() {
        return profiling;
    }
}
