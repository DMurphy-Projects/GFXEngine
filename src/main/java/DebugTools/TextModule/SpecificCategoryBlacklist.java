package DebugTools.TextModule;

public class SpecificCategoryBlacklist extends BaseTextModule {

    String className;
    Integer[] blacklist;

    public SpecificCategoryBlacklist(String name, Integer... cat)
    {
        super();
        blacklist = cat;
        className = name;
    }

    public SpecificCategoryBlacklist(BaseTextModule wrap, String name, Integer... cat)
    {
        super(wrap);
        blacklist = cat;
        className = name;
    }

    @Override
    public boolean allow(String className, int category) {
        if (className.equals(this.className)) {
            if (contains(blacklist, category)) {
                return false;
            }
        }
        return super.allow(className, category);
    }
}
