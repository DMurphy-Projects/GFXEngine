package GxEngine3D.View;

public class PIPView {

    ViewHandler viewHandler;
    int[] position;

    public PIPView(int[] pos, ViewHandler vH)
    {
        viewHandler = vH;
        position = pos;
    }
}
