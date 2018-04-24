package DebugTools.TextModule;

public class GlobalCategoryBlacklist extends BaseTextModule {
    Integer[] blacklist;
    public GlobalCategoryBlacklist(Integer... cat) {
        super();
        blacklist = cat;
    }
    public GlobalCategoryBlacklist(BaseTextModule wrapped, Integer... cat)
    {
        super(wrapped);
        blacklist = cat;
    }

    @Override
    public boolean allow(String className, int category) {
        if (contains(blacklist, category)) {
            return false;
        }
        return super.allow(className, category);
    }
}
