package Scripting;

import GxEngine3D.Camera.Camera;
import GxEngine3D.Camera.ICameraEventListener;
import GxEngine3D.Controller.Scene;
import GxEngine3D.View.ViewHandler;
import Shapes.BaseShape;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

public class SceneLoader {
    public void load(ViewHandler vH, String fileName)
    {
        File file = new File(getClass().getResource(fileName).getFile());

        BaseShape previousShape = null;
        Scene scene = vH.getScene();
        Camera camera = vH.getCamera();

        try(BufferedReader br = new BufferedReader(new FileReader(file))) {
            for(String line; (line = br.readLine()) != null; ) {
                String[] args = line.split(" ");
                if (args[0].startsWith("//"))
                {
                    continue;
                }
                else if (args[0].equals("new"))
                {
                    previousShape = loadObject(args[1], Arrays.copyOfRange(args, 2, args.length));
                    if (previousShape != null)
                    {
                        scene.addObject(previousShape);
                    }
                }
                else if (args[0].equals("set"))
                {
                    double v1 = parseDouble(args[2]);
                    double v2 = parseDouble(args[3]);
                    double v3 = parseDouble(args[4]);
                    if (args[1].equals("scale"))
                    {
                        previousShape.absoluteScale(v1, v2, v3);
                    }
                    else if (args[1].equals("rotate"))
                    {
                        previousShape.absoluteRotate(v1, v2, v3);
                    }
                    else if (args[1].equals("translate"))
                    {
                        previousShape.absoluteTranslate(v1, v2, v3);
                    }
                }
                else if (args[0].equals("add"))
                {
                    double v1 = parseDouble(args[2]);
                    double v2 = parseDouble(args[3]);
                    double v3 = parseDouble(args[4]);
                    if (args[1].equals("scale"))
                    {
                        previousShape.scale(v1, v2, v3);
                    }
                    else if (args[1].equals("rotate"))
                    {
                        previousShape.rotate(v1, v2, v3);
                    }
                    else if (args[1].equals("translate"))
                    {
                        previousShape.translate(v1, v2, v3);
                    }
                }
                else if (args[0].equals("bind"))
                {
                    if (args[1].equals("camera"))
                    {
                        camera.add ((ICameraEventListener)previousShape);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static double parseDouble(String s)
    {
        if (s.startsWith("PI"))
        {
            double d = Math.PI;
            char[] sArr = s.toCharArray();
            String number = "";
            char lastSymbol = Character.MIN_VALUE;
            for (int i=2;i<sArr.length;i++)
            {
                if (Character.isDigit(sArr[i]))
                {
                    number += sArr[i];
                }
                else
                {
                    if (!number.equals("")) {
                        if (lastSymbol == '/') {
                            d /= Double.parseDouble(number);
                        }
                        else if(lastSymbol == '*')
                        {
                            d *= Double.parseDouble(number);
                        }
                        number = "";
                    }
                    //this is only to neatly end the expression
                    if (sArr[i] != ';') {
                        lastSymbol = sArr[i];
                    }
                    continue;
                }
            }
            return d;
        }
        else
        {
            return Double.parseDouble(s);
        }
    }

    //TODO support multiple constructor parameters
    private static BaseShape loadObject(String className, String[] s)
    {
        try {
            Class<?> clazz = Class.forName(className);
            Constructor<?> constructor = clazz.getConstructor(Color.class);
            Object instance = constructor.newInstance(Color.decode(s[0]));
            return (BaseShape) instance;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }
}
