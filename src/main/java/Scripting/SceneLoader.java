package Scripting;

import DebugTools.TextOutput;
import GxEngine3D.Animation.Animator;
import GxEngine3D.Animation.Routines.IRoutine;
import GxEngine3D.Camera.ICameraEventListener;
import GxEngine3D.Controller.GXController;
import GxEngine3D.Controller.ITickListener;
import GxEngine3D.View.ViewHandler;
import Shapes.BaseShape;
import Shapes.IManipulable;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class SceneLoader {

    ArrayList<String> tokens;
    ViewHandler vH;
    int pos;

    HashMap<String, Object> references = new HashMap<>();
    Object previous = null;

    GXController controller;

    public SceneLoader(GXController c)
    {
        controller = c;
    }

    public void load(ViewHandler vH, String fileName)
    {
        File file = new File(getClass().getResource(fileName).getFile());
        this.vH = vH;
        pos = 0;
        tokens = new ArrayList<>();

        try(BufferedReader br = new BufferedReader(new FileReader(file))) {
            for(String line; (line = br.readLine()) != null; ) {
                String[] args = line.split(" ");
                tokens.addAll(Arrays.asList(args));
                tokens.add("\n");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        tokens.add("/eof");

        String t = nextToken();
        while(t != null)
        {
            try {
                newLine(t);
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            t = nextToken();
        }
    }

    public String nextToken()
    {
        String s;
        try {
            s = tokens.get(pos);
            pos++;
        }
        catch (IndexOutOfBoundsException e)
        {
            s = null;
        }
        return s;
    }
    public String peekToken()
    {
        return tokens.get(pos);
    }

    private void newLine(String t) throws InvocationTargetException, NoSuchMethodException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        switch(t)
        {
            case "let":
                let(nextToken());
                break;
            case "new":
                previous = object(t);
                break;
            case "bind":
                bind(nextToken());
                break;
            case "add":
                add(nextToken());
                break;
            case "set":
                set(nextToken());
                break;
        }
    }

    private void add(String s)
    {
        Object o = reference(s);
        s = nextToken();
        if (s.equals("scale") || s.equals("rotate") || s.equals("translate"))
        {
            IManipulable m = (IManipulable) o;
            double v1 = parseDouble(nextToken()), v2 = parseDouble(nextToken()), v3 = parseDouble(nextToken());
            if (s.equals("scale"))
            {
                m.scale(v1, v2, v3);
            }
            else if (s.equals("rotate"))
            {
                m.rotate(v1, v2, v3);
            }
            else
            {
                m.translate(v1, v2, v3);
            }
        }
        else if (s.equals("routine") || s.equals("animator"))
        {

            Animator a = (Animator) reference(nextToken());
            if (s.equals("routine")) {
                IRoutine r = (IRoutine) o;
                a.add(r);
            }
            else
            {
                IManipulable m = (IManipulable) o;
                a.add(m);
            }
        }
        else
        {
            vH.getScene().addObject((BaseShape) o);
            ((BaseShape) o).init();
        }
    }

    private void set(String s)
    {
        Object o = reference(s);
        s = nextToken();
        if (s.equals("scale") || s.equals("rotate") || s.equals("translate"))
        {
            //we know we're dealing with a manipulable object
            IManipulable m = (IManipulable) o;
            double v1 = parseDouble(nextToken()), v2 = parseDouble(nextToken()), v3 = parseDouble(nextToken());
            if (s.equals("scale"))
            {
                m.absoluteScale(v1, v2, v3);
            }
            else if (s.equals("rotate"))
            {
                m.absoluteRotate(v1, v2, v3);
            }
            else
            {
                m.absoluteTranslate(v1, v2, v3);
            }
        }
    }

    private void bind(String s)
    {
        switch (s)
        {
            case "camera":
                s = nextToken();
                assert isReference(s);
                vH.getCamera().add((ICameraEventListener) reference(s));
                break;
            case "controller":
                s = nextToken();
                assert isReference(s);
                switch (nextToken())
                {
                    case "pre":
                        controller.addPreListener((ITickListener) reference(s));
                        break;
                    case "tick":
                        controller.add((ITickListener) reference(s));
                        break;
                    case "post":
                        controller.addPostListener((ITickListener) reference(s));
                        break;
                    default:
                        controller.add((ITickListener) reference(s));
                        break;
                }
                break;
        }
    }

    private void let(String s) throws InvocationTargetException, NoSuchMethodException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        //the next token in let is a name, must be alpha-numeric
        assert isAlphaNumeric(s);
        String t = nextToken();
        assert t.equals("=");
        previous = object(nextToken());
        references.put(s, previous);
    }

    private boolean isObject(String s)
    {
        return s.equals("new");
    }

    private Object object(String s) throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException, ClassNotFoundException {
        assert isObject(s);//new
        String name = nextToken();
        Class<?> clazz = Class.forName(name);

        assert isClassName(name);
        String[] constructor = constructor(peekToken());
        Class[] _classes = getConstructorDefinition(constructor);

        Constructor<?> _constructor = clazz.getConstructor(_classes);
        Object instance = _constructor.newInstance(getConstructorInstances(constructor, _classes));
        return instance;
    }

    //should pass in the peeked token
    private String[] constructor(String t) throws InvocationTargetException, NoSuchMethodException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        ArrayList<String> c = new ArrayList<>();
        while(!t.equals("let") && !t.equals("bind") && !t.equals("/eof") && !t.startsWith("//") && !t.equals("\n"))
        {
            if (isReference(t))
            {
                t = nextToken();
                c.add(t);
            }
            else {
                String s = nextToken();
                if (s.equals("{"))
                {
                    c.add(array(s));
                }
                else {
                    c.add(s);
                }
            }
            t = peekToken();
        }
        String[] arr = new String[c.size()];
        c.toArray(arr);
        return arr;
    }

    private String array(String t)
    {
        String arr = t  + " ";
        while(!(t = nextToken()).equals("}"))
        {
            arr += t + " ";
        }
        arr += t;
        return arr;
    }

    private boolean isReference(String s)
    {
        if (s.startsWith("*"))
        {
            String _s = s.substring(1, s.length());//remove the *
            return references.containsKey(_s);
        }
        else if(s.equals("/prev"))
        {
            return true;
        }
        return false;
    }

    private Object reference(String s)
    {
        if (s.equals("/prev"))
        {
            return previous;
        }
        else {
            assert isReference(s);
            return references.get(s.substring(1, s.length()));
        }
    }

    private boolean isClassName(String s)
    {
        try {
            Class.forName(s);
        } catch (ClassNotFoundException e) {
            return false;
        }
        return true;
    }

    private double parseDouble(String s)
    {
        if (s.contains("PI"))
        {
            double d = Math.PI;
            char[] sArr = s.toCharArray();
            String number = "";
            char lastSymbol = Character.MIN_VALUE;
            for (int i=0;i<sArr.length;i++)
            {
                if (Character.isDigit(sArr[i]))
                {
                    number += sArr[i];
                }
                else if(sArr[i] == 'P')
                {
                    //skip past the pi
                    i++;
                }
                else if (sArr[i] == '-' && i == 0)
                {
                    d *= -1;
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

    private Class[] getConstructorDefinition(String[] _s) throws ClassNotFoundException {
        Class[] _classes = new Class[_s.length];
        for (int i = 0;i<_s.length;i++)
        {
            String s = _s[i];
            _classes[i] = getType(s);
        }
        return _classes;
    }

    private Class getType(String s) throws ClassNotFoundException {
        boolean specificCast;
        //specific cast
        if ((specificCast = s.startsWith("(") && s.contains(")")) || isReference(s))
        {
            String name = s;
            if (specificCast) {
                name = s.substring(1, s.indexOf(")"));
            }
            else
            {
                name = reference(s).getClass().getName();
            }
            Class<?> clazz = Class.forName(name);
            return clazz;
        }
        else
        {
            //try to infer the type
            if (s.startsWith("#"))
            {
                return Color.class;
            }
            else if (isInteger(s))
            {
                return Integer.class;
            }
            else if (isDouble(s))
            {
                return Double.class;
            }
            else if (s.startsWith("{"))
            {
                //is array
                String[] arr = s.replace("{ ", "").replace(" }", "").split(" ");
                Class type = null;
                for (String arrS:arr)
                {
                    Class cur = getType(arrS);
                    if (type != null && type != cur)
                    {
                        throw new IllegalArgumentException(type + " " + cur);
                    }
                    else
                    {
                        type = cur;
                    }
                }
                type = Class.forName("[L" +type.getName() + ";");
                return type;
            }
            else
            {
                //fallback type
                return String.class;
            }
        }
    }

    private Object[] getConstructorInstances(String[] _s, Class[] _classes) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Object[] _instances = new Object[_s.length];
        for (int i = 0;i<_s.length;i++)
        {
            String s = _s[i];
            _instances[i] = getInstance(s, _classes[i]);
        }
        return _instances;
    }

    private Object getInstance(String s, Class c) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if (s.startsWith("(") && s.contains(")"))
        {
            s = s.substring(s.indexOf(")")+1, s.length());
        }

        String cName = c.getName();
        if (cName.equals(Color.class.getName()))
        {
            return Color.decode(s);
        }
        else if (cName.equals(Double.class.getName()))
        {
            return parseDouble(s);
        }
        else if (cName.equals(Integer.class.getName()))
        {
            return Integer.parseInt(s);
        }
        else if(isReference(s))
        {
           return reference(s);
        }
        else if (s.startsWith("{"))
        {
            String[] arr = s.replace("{ ", "").replace(" }", "").split(" ");
            Class type = Class.forName(cName.replace("[L", "").replace("/", ".").replace(";", ""));
            Object o = Array.newInstance(type, arr.length);
            for (int ii=0;ii<arr.length;ii++)
            {
                Array.set(o, ii, getInstance(arr[ii], type));
            }
            return o;
        }
        else
        {
            Class<?> clazz = Class.forName(s);
            Constructor<?> constructor = clazz.getConstructor();
            return constructor.newInstance();
        }
    }

    private static boolean isAlphaNumeric(String s)
    {
        for (char c:s.toCharArray())
        {
            if (!Character.isDigit(c) && !Character.isAlphabetic(c))
            {
                return false;
            }
        }
        return true;
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
            if (!Character.isDigit(c) && c != '.' && !s.startsWith("PI") && !s.startsWith("-"))
            {
                return false;
            }
        }
        return true;
    }
}
