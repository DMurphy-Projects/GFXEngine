package DebugTools.TextModule;

public class BaseTextModule {

    BaseTextModule wrapped;
    public BaseTextModule()
    {
        wrapped = null;
    }
    public BaseTextModule(BaseTextModule wrap)
    {
        wrapped = wrap;
    }
    //returning to super should happen last
    public boolean allow(String className, int category)
    {
        //if there are now more wrapped, don't affect the outcome
        if (wrapped == null) return true;
        return wrapped.allow(className, category);
    }

    protected <T> boolean contains(T[] arr, T test)
    {
        for (T s:arr)
        {
            if (test.equals(s))
            {
                return true;
            }
        }
        return false;
    }
}
