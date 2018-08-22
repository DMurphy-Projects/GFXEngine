package DebugTools.TextModule;

public class TextToggle extends BaseTextModule {
    boolean toggle;
    public TextToggle(boolean b)
    {
        super();
        toggle = b;
    }
    public TextToggle(BaseTextModule wrap, boolean b)
    {
        super(wrap);
        toggle = b;
    }
    @Override
    public boolean allow(String className, int category) {
        if (toggle) {
            return super.allow(className, category);
        }
        return false;
    }
}
