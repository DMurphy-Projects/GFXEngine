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
                    //is a comment
                    continue;
                }
                else if (args[0].equals("new"))
                {
                    previousShape = loadObject(args[1], Arrays.copyOfRange(args, 2, args.length));
                    previousShape.init();
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

    private static BaseShape loadObject(String className, String[] s)
    {
        try {
            Class<?> clazz = Class.forName(className);
            Class[] _classes = getConstructorDefinition(s);
            Constructor<?> constructor = clazz.getConstructor(_classes);
            Object instance = constructor.newInstance(getConstructorInstances(s, _classes));
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

    private static Class[] getConstructorDefinition(String[] _s)
    {
        Class[] _classes = new Class[_s.length];
        for (int i = 0;i<_s.length;i++)
        {
            String s = _s[i];
            //specific cast
            if ((s.startsWith("(") && s.contains(")")) || (s.startsWith("<") && s.endsWith(">")))
            {
                int end = s.contains(")") ? s.indexOf(")") : s.indexOf(">");
                String name = s.substring(1, end);
                try {
                    Class<?> clazz = Class.forName(name);
                    _classes[i] = clazz;
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            else
            {
                //try to infer the type
                if (s.startsWith("#"))
                {
                    _classes[i] = Color.class;
                }
                else if (isInteger(s))
                {
                    _classes[i] = Integer.class;
                }
                else if (isDouble(s))
                {
                    _classes[i] = Double.class;
                }
                else
                {
                    //fallback type
                    _classes[i] = String.class;
                }
            }
        }
        return _classes;
    }

    private static Object[] getConstructorInstances(String[] _s, Class[] _classes) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Object[] _instances = new Object[_s.length];
        for (int i = 0;i<_s.length;i++)
        {
            String s = _s[i];
            if (s.startsWith("(") && s.contains(")"))
            {
                s = s.substring(s.indexOf(")")+1, s.length());
            }
            else if (s.startsWith("<") && s.endsWith(">"))
            {
                s = s.substring(1, s.length()-1);
            }
            String cName = _classes[i].getName();
            if (cName.equals(Color.class.getName()))
            {
                _instances[i] = Color.decode(s);
            }
            else if (cName.equals(Double.class.getName()))
            {
                _instances[i] = parseDouble(s);
            }
            else if (cName.equals(Integer.class.getName()))
            {
                _instances[i] = Integer.parseInt(s);
            }
            else
            {
                Class<?> clazz = Class.forName(s);
                Constructor<?> constructor = clazz.getConstructor();
                _instances[i] = constructor.newInstance();
            }
        }
        return _instances;
    }

    private static boolean isInteger(String s)
    {
        for (char c:s.toCharArray())
        {
            if (!Character.isDigit(c))
            {
                return false;
            }
        }
        return true;
    }

    private static boolean isDouble(String s)
    {
        for (char c:s.toCharArray())
        {
            if (!Character.isDigit(c) && c != '.')
            {
                return false;
            }
        }
        return true;
    }
}
