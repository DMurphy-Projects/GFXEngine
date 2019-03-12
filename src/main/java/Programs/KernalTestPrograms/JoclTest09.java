package Programs.KernalTestPrograms;

import DebugTools.PaintPad;
import GxEngine3D.Camera.Camera;
import GxEngine3D.Helper.*;
import GxEngine3D.Model.Matrix.Matrix;
import TextureGraphics.BarycentricGpuRender_v3;
import TextureGraphics.BarycentricGpuRender_v4;
import TextureGraphics.Memory.Texture.JoclTexture;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

//This is the example of using a method that uses inverse matrices
//NOTE: being close to the polygon causes strange warping/shifting of the polygon as a whole. This is believed to be caused by the camera's near plane intersecting the shape
//      resulting in a shape that is distorted. This is also exists in earlier tests so is not caused by recent changes. This does not exist in the sampling based method
//      as it does not use clip points of the polygon directly
//      Is this intended or a bug of unknown origin?
//      Are there ways of minimising/working around this?
public class JoclTest09
{
    public static void main(String args[])
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                new JoclTest09(500,500);
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

    double[][] translate, rotate, scale;
    Matrix projection, combined, allCombined, textureMap;

    BarycentricGpuRender_v4 renderer;//is a specific test
    JoclTexture texture;

    Map<Camera.Direction, Boolean> keys = new HashMap<>();

    PerformanceTimer t;

    PaintPad debugPad;

    public JoclTest09(int width, int height)
    {
        debug = SUCCINCT;

        PerformanceTimer.Mode mode = debug == VERBOSE?
                PerformanceTimer.Mode.Percentage:
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

        renderer = new BarycentricGpuRender_v4(screenWidth, screenHeight);
        texture = renderer.createTexture("resources/Textures/default.png");

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

        PolygonPointSampler.initDebug();

//        debugPad = new PaintPad(screenWidth, screenHeight);
//        debugPad.setMode(PaintPad.Mode.Centre);
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
        matrix = new Matrix(matrix.matrixMultiply(MatrixHelper.setupTranslateMatrix(.5, 0.5, 0)));
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

        camera = new Camera(0,0, 2);
        camera.lookAt(new double[]{0, 0, 0});
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

        allCombined = new Matrix(combined.matrixMultiply(projection));

        for (double[][] poly:polys)
        {
            double[][] clipPoints = new double[poly.length][3];
            for (int i=0;i<poly.length;i++) {
                clipPoints[i] = MatrixHelper.applyExplicitMatrix(allCombined, poly[i]);
            }
            clipPolys.add(clipPoints);
        }

//        debugPad.init();
//        debugPad.drawPolygon(debugPad.createPolygon(clipPolys.get(0), screenWidth/10, screenHeight/10), Color.BLACK, false);
//        debugPad.finish();
    }

    private void updateScreen()
    {
        t.time();
        updateScene();

        t.time();
        renderer.setup();
        //note that in this example the implicit matrix is the same for all shapes, this will not be the case in the final version
        Matrix renderInverse = new Matrix(allCombined.inverse_4x4());
        renderer.setInverseMatrix(renderInverse.flatten());

        t.time();
        for (int i=0;i<clipPolys.size();i++)
        {
            renderPolygon(clipPolys.get(i), polys.get(i), tAnchors.get(i), renderInverse, textureMap);
        }
        t.time();

        image = renderer.createImage();
        t.time();

        if (debug == VERBOSE) {
            t.printNextTime("Scene Update Took");
            t.printNextTime("Renderer Setup Took");
            t.printNextTime("Enqueue Took");
            t.printNextTime("Image Creation Took");
            System.out.println();
        }
        else if (debug == SUCCINCT) {
            t.printNextTime("Total Took");
        }

        t.reset();
        imageComponent.repaint();
    }

    private void renderPolygon(double[][] cPolygon, double[][] rPolygon, double[][] tPolygon, Matrix invRender, Matrix textureMatrix)
    {
        if (PolygonClipBoundsChecker.shouldCull(cPolygon)) return;

        if (PolygonClipBoundsChecker.intersectsNearPlane(cPolygon)) {
            double[][] newData = PolygonPointSampler.recreateNearPlaneClipPolygon(cPolygon);
            if (newData.length != 0) {
                cPolygon = newData;

                rPolygon = new double[cPolygon.length][];
                for (int i = 0; i < cPolygon.length; i++) {
                    rPolygon[i] = MatrixHelper.applyExplicitMatrix(invRender, cPolygon[i]);
                }

                tPolygon = new double[cPolygon.length][];
                for (int i = 0; i < cPolygon.length; i++) {
                    tPolygon[i] = MatrixHelper.applyImplicitMatrix(textureMatrix, rPolygon[i]);
                }
            }
        }

        renderer.setRelativePoly(rPolygon);
        renderer.render(cPolygon, tPolygon, texture);
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

        Matrix frustum = FrustumMatrixHelper.createMatrix(90, 0.1, 30, screenWidth, screenHeight, 1);
        this.projection = new Matrix(frustum.matrixMultiply(camera.getMatrix()));
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

