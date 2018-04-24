package DebugTools.TextModule;

public class TextWhitelist extends BaseTextModule {
    String[] whitelist;
    public TextWhitelist(String... l)
    {
        super();
        whitelist = l;
    }
    public TextWhitelist(BaseTextModule wrap, String... l)
    {
        super(wrap);
        whitelist = l;
    }

    @Override
    public boolean allow(String className, int category) {
        if (contains(whitelist, className)) {
            return super.allow(className, category);
        }
        return false;
    }
}
