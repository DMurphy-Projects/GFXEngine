package DebugTools.TextModule;

public class SubCategoryBlacklist extends BaseTextModule {

    String className;
    Integer[] blacklist;

    public SubCategoryBlacklist(String name, Integer... cat)
    {
        super();
        blacklist = cat;
    }

    public SubCategoryBlacklist(BaseTextModule wrap, String name, Integer... cat)
    {
        super(wrap);
        blacklist = cat;
    }

    @Override
    public boolean allow(String className, int category) {
        if (!contains(blacklist, category)) {
            return super.allow(className, category);
        }
        return false;
    }
}
