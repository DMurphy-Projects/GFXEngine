package Programs.KernalTestPrograms;

import GxEngine3D.Camera.Camera;
import GxEngine3D.Helper.Maths.FrustumMatrixHelper;
import GxEngine3D.Helper.Maths.MatrixHelper;
import GxEngine3D.Helper.PerformanceTimer;
import GxEngine3D.Model.Matrix.Matrix;
import TextureGraphics.GpuRendererFragment;
import TextureGraphics.GpuRendererIterateColorPolygon;
import TextureGraphics.UpdateScene;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class JoclTest12 {

    private BufferedImage image;
    private JFrame frame;
    private JPanel imageComponent;

    private int screenWidth = 0;
    private int screenHeight = 0;

    Camera camera;

    ArrayList<double[][]> relativePolys;

    double[][] translate, rotate, scale;
    Matrix frustrum, projection, combined, allCombined;

    UpdateScene updater;
    GpuRendererFragment renderer;

    double NEAR = 0.1, FAR = 30;

    Map<Camera.Direction, Boolean> keys = new HashMap<>();

    int[] colorArray = new int[]{
            Color.RED.getRGB(),
            Color.BLUE.getRGB(),
            Color.GREEN.getRGB(),
    };

    String VERBOSE = "v", SUCCINCT = "s";
    String debug;
    PerformanceTimer t;

    public static void main(String[] arg)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                new JoclTest12(480,480);
            }
        });
    }

    public JoclTest12(int width, int height)
    {
        debug = VERBOSE;

        PerformanceTimer.Mode mode = debug == VERBOSE?
                PerformanceTimer.Mode.Nano:
                PerformanceTimer.Mode.Total;

        t = new PerformanceTimer(mode);

        screenWidth = width;
        screenHeight = height;

        updater = new UpdateScene(screenWidth, screenHeight);;
        renderer = new GpuRendererFragment(screenWidth, screenHeight, updater);

        initScene();
        addPolygons();

        setupMatrices();

        updateRelativePolygons();

        imageComponent = new JPanel()
        {
            private static final long serialVersionUID = 1L;
            public void paintComponent(Graphics g)
            {
                super.paintComponent(g);
                g.drawImage(image, 0,0,this);
            }
        };

        // Create the main frame
        frame = new JFrame("Multiple Barycentric");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        imageComponent.setPreferredSize(new Dimension(width, height));
        frame.add(imageComponent, BorderLayout.CENTER);
        frame.pack();

        initInteraction();

        frame.setVisible(true);

        updateScreen();
    }

    private void initInteraction()
    {
        frame.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                switch (e.getKeyChar())
                {
                    case 'p':
                        camera.lookAt(new double[]{0, 0, 0});
                        updateScreen();
                        break;
                    case 'i':
                        break;
                    case 'l':
                        updateScreen();
                        break;
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyChar())
                {
                    case 'w':
                        keys.put(Camera.Direction.UP, true); break;
                    case 'a':
                        keys.put(Camera.Direction.LEFT, true); break;
                    case 's':
                        keys.put(Camera.Direction.DOWN, true); break;
                    case 'd':
                        keys.put(Camera.Direction.RIGHT, true); break;
                }
                camera.CameraMovement(keys);
                updateScreen();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyChar())
                {
                    case 'w':
                        keys.put(Camera.Direction.UP, false); break;
                    case 'a':
                        keys.put(Camera.Direction.LEFT, false); break;
                    case 's':
                        keys.put(Camera.Direction.DOWN, false); break;
                    case 'd':
                        keys.put(Camera.Direction.RIGHT, false); break;
                }
                camera.CameraMovement(keys);
                updateScreen();
            }
        });

        imageComponent.addMouseWheelListener(new MouseWheelListener()
        {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e)
            {

            }
        });

        imageComponent.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                Point p = frame.getLocationOnScreen();
                int[] center = new int[]{
                        p.x + (screenWidth/2),
                        p.y + (screenHeight/2)
                };

                camera.MouseMovement(center[0] - e.getXOnScreen(), center[1] - e.getYOnScreen());
                centreMouse();

                updateScreen();
            }
        });
    }

    public void centreMouse() {
        try {
            Robot r = new Robot();

            Point loc = frame.getLocationOnScreen();

            r.mouseMove(loc.x + (screenWidth/2), loc.y + (screenHeight/2));
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    private void updateRelativePolygons()
    {
        updater.prepare(relativePolys);

        updater.resetPolygons();
        for (double[][] polygon: relativePolys)
        {
            updater.addPolygon(polygon);
        }
    }

    private void updateSceneGpu()
    {
        createProjectionMatrix();

        allCombined = new Matrix(combined.matrixMultiply(projection));

        updater.setup();

        updater.setMatrix(allCombined.flatten());

        updater.enqueue();

        updater.readData();
    }

    private void updateScreen()
    {
        t.time();
        updateSceneGpu();
        t.time();

        renderer.setup();

        renderer.setupPolygonArray(updater.getScreenPolygonArray());
        renderer.setupPolygonStartArray(updater.getPolygonStartArray());
        t.time();

        renderer.setColor(colorArray);
        updater.waitOnReadTasks();
        t.time();

        DoubleBuffer clipBuffer = updater.getClipPolygonBuffer();
        DoubleBuffer screenBuffer = updater.getScreenPolygonBuffer();

        renderer.prepare(clipBuffer, screenBuffer, updater.getPolygonStartData());
        t.time();

        renderer.setupRender();
        t.time();

        renderer.enqueueTasks(updater.getTask());
        t.time();

        image = renderer.createImage();
        t.time();

        imageComponent.repaint();

        updater.finish();

        if (debug == SUCCINCT)
        {
            t.printNextTime("Total Took");
        }
        else if (debug == VERBOSE)
        {
            t.printNextTime("GPU Scene Update");
            t.printNextTime("Renderer Program Setup");
            t.printNextTime("Wait On Reading");
            t.printNextTime("Renderer Preparing");
            t.printNextTime("Render Setup");
            t.printNextTime("Renderer Enqueue");
            t.printNextTime("Renderer Create");

            System.out.println();
        }

        t.reset();
    }

    private void addPolygons()
    {
        double[][] relativePoints = new double[][]{
                new double[]{-.5, -.5, 0},
                new double[]{.5, -.5, 0},
                new double[]{.5, .5, 0},
                new double[]{-.5, .5, 0},
        };

        wallPanelScene(relativePoints);
    }

    private void wallPanelScene(double[][] relativePoints)
    {
        int width = 100;
        int height = 100;

        double[][] translate = MatrixHelper.setupTranslateMatrix(1, 0, 0);

        Matrix horizontal = new Matrix(translate);

        translate = MatrixHelper.setupTranslateMatrix(-width, 1, 0);
        Matrix vertical = new Matrix(translate);

        for (int i=0;i<height;i++) {
            for (int ii=0;ii<width;ii++) {
                relativePoints = Arrays.copyOfRange(relativePoints, 0, relativePoints.length);
                applyMatrix(relativePoints, horizontal);

                addPolygon(relativePoints);
            }
            relativePoints = Arrays.copyOfRange(relativePoints, 0, relativePoints.length);
            applyMatrix(relativePoints, vertical);
        }
    }

    private void addPolygon(double[][] polygon)
    {
        relativePolys.add(polygon);
    }

    private void applyMatrix(double[][] relativePoints, Matrix combined)
    {
        for (int i=0;i<relativePoints.length;i++) {
            relativePoints[i] = MatrixHelper.applyImplicitMatrix(combined, relativePoints[i]);
        }
    }

    private void initScene()
    {
        relativePolys = new ArrayList<>();

        camera = new Camera(5,5, 10);
    }

    private void updateCamera()
    {
        camera.setup();
    }

    private void setupMatrices()
    {
        createProjectionMatrix();

        translate = MatrixHelper.setupTranslateMatrix(0, 0, 0);
        rotate = MatrixHelper.setupFullRotation(0, 0, 0);
        scale = MatrixHelper.setupTranslateMatrix(0, 0, 0);

        createCombinedMatrix();
    }

    private void createCombinedMatrix()
    {
        Matrix combined = new Matrix(translate);
        combined = new Matrix(combined.matrixMultiply(rotate));
        combined = new Matrix(combined.matrixMultiply(scale));
        this.combined = combined;
    }

    private void createProjectionMatrix()
    {
        updateCamera();

        Matrix frustum = FrustumMatrixHelper.createMatrix(75, NEAR, FAR, screenWidth, screenHeight, 1);
        this.projection = new Matrix(frustum.matrixMultiply(camera.getMatrix()));

        this.frustrum = frustum;
    }
}
