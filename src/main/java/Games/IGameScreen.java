package Games;

public interface IGameScreen {

    void setPixel(int x, int y, int color);
    int getPixel(int x, int y);

    int[] getScreenSize();
}
