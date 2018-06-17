package GxEngine3D.Model;


public class RefBoolean {
    private boolean b;

    public RefBoolean(boolean b)
    {
        set(b);
    }

    public void set(boolean b)
    {
        this.b = b;
    }
    public boolean get()
    {
        return b;
    }
}
