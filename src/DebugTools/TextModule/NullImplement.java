package DebugTools.TextModule;

public class NullImplement extends BaseTextModule {
    @Override
    public boolean allow(String className, int category) {
        return false;
    }
}
