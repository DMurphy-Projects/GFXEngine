package Games.Ant;

import Games.IGame;
import Games.IGameScreen;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

public class AntGame implements IGame{

    IGameScreen screen;

    int x, y, facing = 0, width, height;

    HashMap<Integer, Boolean> actionMap = new HashMap<>();
    ArrayList<Integer> colorOrder = new ArrayList<>();

    boolean isInBounds = true;

    public AntGame(IGameScreen screen)
    {
        this.screen = screen;
        int[] sizes = screen.getScreenSize();
        width = sizes[0];
        height = sizes[1];

        x = width/2;
        y = height/2;
    }

    public void addAction(Color color, boolean turnLeft)
    {
        int intColor = 0x00000000 | ((color.getRed() << 16) & 0x00FF0000) | ((color.getGreen() << 8) & 0x0000FF00) | (color.getBlue() & 0x000000FF);
        colorOrder.add(intColor);
        actionMap.put(intColor, turnLeft);
    }

    @Override
    public void tick() {
        if (!isInBounds) return;

        moveForward();

        isInBounds = checkBounds(x, y);
        if (!isInBounds) return;

        int current = screen.getPixel(x, y);
        screen.setPixel(x, y, getNextColor(current));

        boolean shouldTurnLeft = actionMap.get(current);

        if (shouldTurnLeft)
        {
            turnLeft();
        }
        else
        {
            turnRight();
        }
    }

    private boolean checkBounds(int x, int y)
    {
        return x > 0 && y > 0 && x < width && y < height;
    }
    private int getNextColor(int color)
    {
        int i=0;
        for (int c:colorOrder)
        {
            if (c == color)
            {
                break;
            }
            i++;
        }
        i++;
        if (i > colorOrder.size()-1)
        {
            i = 0;
        }
        return colorOrder.get(i);
    }

    private void moveForward()
    {
        switch (facing)
        {
            case 0:
                x++; break;
            case 1:
                y++; break;
            case 2:
                x--; break;
            case 3:
                y--; break;
        }
    }

    private void turnLeft()
    {
        facing++;
        if (facing > 3) facing = 0;
    }

    private void turnRight()
    {
        facing--;
        if (facing < 0) facing = 3;
    }
}
