package DebugTools.TextModule;

public class TextBlacklist extends BaseTextModule {
    String[] blacklist;
    public TextBlacklist(String[] l) {
        super();
        blacklist = l;
    }
    public TextBlacklist(BaseTextModule wrap, String[] l) {
        super(wrap);
        blacklist = l;
    }

    @Override
    public boolean allow(String className, int category) {
        if (!contains(blacklist, className))
        {
            return super.allow(className, category);
        }
        return false;
    }
}
