package Programs.KernalTestPrograms;

import GxEngine3D.Camera.Camera;
import GxEngine3D.Helper.Maths.FrustumMatrixHelper;
import GxEngine3D.Helper.Maths.MatrixHelper;
import GxEngine3D.Helper.PerformanceTimer;
import GxEngine3D.Model.Matrix.Matrix;
import TextureGraphics.IterateMethods.GpuRendererIterateColorPolygon;
import TextureGraphics.Memory.Texture.JoclTexture;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

//NOTE: there are 2 variants of this method
//this method uses bulk memory writes
//this method is ~6 times faster than the other variant whilst at high triangle counts, ie scales much better
public class JoclTest10 {
    public static void main(String args[])
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                new JoclTest10(480,480);
            }
        });
    }

    String VERBOSE = "v", SUCCINCT = "s";
    String debug;

    private int screenWidth = 0;
    private int screenHeight = 0;

    private BufferedImage image;
    private JFrame frame;
    private JPanel imageComponent;

    Camera camera;
    ArrayList<double[][]> polys;
    ArrayList<double[][]> tAnchors;

    ArrayList<double[][]> clipPolys;

    ArrayList<double[][]> screenPolys;

    double[][] translate, rotate, scale;
    Matrix frustrum, projection, combined, allCombined, textureMap;

    GpuRendererIterateColorPolygon renderer;//is a specific test
    JoclTexture texture;

    Map<Camera.Direction, Boolean> keys = new HashMap<>();

    PerformanceTimer t;

    double NEAR = 0.1, FAR = 30;

    int[] colorArray = new int[]{
            Color.RED.getRGB(),
            Color.BLUE.getRGB(),
            Color.GREEN.getRGB(),
    };

    public JoclTest10(int width, int height)
    {
        debug = VERBOSE;

        PerformanceTimer.Mode mode = debug == VERBOSE?
                PerformanceTimer.Mode.Nano:
                PerformanceTimer.Mode.Total;

        t = new PerformanceTimer(mode);

        screenWidth = width;
        screenHeight = height;

        // Create the image and the component that will paint the image
        imageComponent = new JPanel()
        {
            private static final long serialVersionUID = 1L;
            public void paintComponent(Graphics g)
            {
                super.paintComponent(g);
                g.drawImage(image, 0,0,this);
            }
        };

        renderer = new GpuRendererIterateColorPolygon(screenWidth, screenHeight);
//        texture = renderer.createTexture("resources/Textures/default.png");

        initScene();
        addPolygons();

        setupMatrices();

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
                    case 'o':
                        System.out.println("Polygon");
                        for (double[][] poly:clipPolys)
                        {
                            for (double[] point:poly)
                            {
                                System.out.println(Arrays.toString(point));
                            }
                        }
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
                Point p = imageComponent.getLocationOnScreen();
                int[] center = new int[]{imageComponent.getWidth()/2, imageComponent.getHeight()/2};

                camera.MouseMovement(center[0] - e.getX() - p.x, center[1] - e.getY() - p.y);
                centreMouse();

                updateScreen();
            }
        });
    }

    //the relative -> texture matrix is case specific, ie each shape needs its own transform
    private void setupTextureMap()
    {
        //TODO do we need to scale inward by a small value?
        Matrix matrix = new Matrix(MatrixHelper.setupIdentityMatrix());
        matrix = new Matrix(matrix.matrixMultiply(MatrixHelper.setupTranslateMatrix(0.5, 0.5, 0)));
        matrix = new Matrix(matrix.matrixMultiply(MatrixHelper.setupScaleMatrix(1, -1, 1)));
        textureMap = matrix;
    }

    private void addPolygons()
    {
        double[][] relativePoints = new double[][]{
                new double[]{-.5, -.5, 0},
                new double[]{.5, -.5, 0},
                new double[]{.5, .5, 0},
                new double[]{-.5, .5, 0},
        };

        setupTextureMap();
        double[][] textureRelativePoints = new double[relativePoints.length][];
        for(int i=0;i<relativePoints.length;i++)
        {
            textureRelativePoints[i] = MatrixHelper.applyImplicitMatrix(textureMap, relativePoints[i]);
        }
        singlePolygonScene(relativePoints, textureRelativePoints);
    }

    private void singlePolygonScene(double[][] relativePoints, double[][] textureRelativePoints)
    {
        addPolygon(relativePoints, textureRelativePoints);
    }

    private void wallPanelScene(double[][] relativePoints, double[][] textureRelativePoints)
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

                addPolygon(relativePoints, textureRelativePoints);
            }
            relativePoints = Arrays.copyOfRange(relativePoints, 0, relativePoints.length);
            applyMatrix(relativePoints, vertical);
        }
    }

    private void intersectionScene(double[][] relativePoints, double[][] textureRelativePoints)
    {
        addPolygon(relativePoints, textureRelativePoints);

        relativePoints = Arrays.copyOfRange(relativePoints, 0, relativePoints.length);

        double[][] rotate = MatrixHelper.setupFullRotation(0, Math.PI/2, 0);
        applyMatrix(relativePoints, new Matrix(rotate));

        addPolygon(relativePoints, textureRelativePoints);
    }

    private void applyMatrix(double[][] relativePoints, Matrix combined)
    {
        for (int i=0;i<relativePoints.length;i++) {
            relativePoints[i] = MatrixHelper.applyImplicitMatrix(combined, relativePoints[i]);
        }
    }

    private void addPolygon(double[][] polygon, double[][] textureAnchor)
    {
        polys.add(polygon);
        tAnchors.add(textureAnchor);
    }

    private void initScene()
    {
        polys = new ArrayList<>();
        tAnchors = new ArrayList<>();
        clipPolys = new ArrayList<>();
        screenPolys = new ArrayList<>();

        camera = new Camera(5,5, 10);
//        camera.lookAt(new double[]{0, 0, 0});
    }

    private void updateCamera()
    {
        camera.setup();
    }

    private void updateScene()
    {
        //this app does not use moving objects, so we only need to update the projection matrix
        createProjectionMatrix();

        clipPolys.clear();
        screenPolys.clear();

        allCombined = new Matrix(combined.matrixMultiply(projection));

        for (double[][] poly:polys)
        {
            double[][] clipPoints = new double[poly.length][3];
            double[][] screenPoints = new double[poly.length][3];

            for (int i=0;i<poly.length;i++) {
                clipPoints[i] = MatrixHelper.applyExplicitMatrix(allCombined, poly[i]);

                //translate into screen space, z remains the same
                //screen_x = (clip_x + 1) * 0.5 * screenWidth
                screenPoints[i][0] = (clipPoints[i][0] + 1) * 0.5 * screenWidth;
                //screen_y =  (1 - (clip_y + 1)* 0.5) * screenHeight
                screenPoints[i][1] = (1 - (clipPoints[i][1] + 1) * 0.5) * screenHeight;
                screenPoints[i][2] = clipPoints[i][2];
            }
            clipPolys.add(clipPoints);
            screenPolys.add(screenPoints);
        }
    }

    private void updateScreen() {

        t.time();
        //TODO this is currently the bottleneck for this kernel
        updateScene();

        t.time();
        renderer.setup();
        t.time();
        renderer.prepare(clipPolys);

        t.time();
        for (int i = 0; i < clipPolys.size(); i++) {
            renderer.setColor(colorArray[i % colorArray.length]);

            renderer.setScreenPoly(screenPolys.get(i));
            renderer.render(clipPolys.get(i), tAnchors.get(i), texture);
        }
        t.time();

        image = renderer.createImage();
        t.time();

        if (debug == VERBOSE) {
            t.printNextTime("Scene Update Took");
            t.printNextTime("Renderer Setup Took");
            t.printNextTime("Renderer Prepare Took");
            t.printNextTime("Enqueue Took");
            t.printNextTime("Image Creation Took");
            System.out.println();
        } else if (debug == SUCCINCT) {
            t.printNextTime("Total Took");

        }

        t.reset();
        imageComponent.repaint();
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

    public void centreMouse() {
        try {
            Robot r = new Robot();

            Point loc = frame.getLocationOnScreen();

            r.mouseMove(loc.x + (screenWidth/2), loc.y + (screenHeight/2));
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }
}
