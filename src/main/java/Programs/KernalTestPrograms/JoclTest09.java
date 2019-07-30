package Programs.KernalTestPrograms;

import DebugTools.PaintPad;
import GxEngine3D.Camera.Camera;
import GxEngine3D.Helper.*;
import GxEngine3D.Helper.Maths.FrustumMatrixHelper;
import GxEngine3D.Helper.Maths.MatrixHelper;
import GxEngine3D.Model.Matrix.Matrix;
import TextureGraphics.BarycentricRender.BarycentricGpuRender_v4;
import TextureGraphics.JoclSetup;
import TextureGraphics.Memory.Texture.JoclTexture;
import TextureGraphics.Memory.Texture.SingleTextureData;

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
//          100% caused by intersection of near plane with the polygon to render, to fix this we would need to clip on the near plane
//          the work of clipping is complete in "PolygonPointSampler" however recreating the texture relative polygon is the current stopping point
//          as it turns out you cannot take the inverse of the frustum matrix which leads to the need for another solution, 3d picking is the current best but seems
//          like a lot of work to do for every clipped polygon each frame
//      A better solution seems like 3d picking the normal vector of the near clipping plane into world space then performing the clip on the relative/world space polygons
//      then transforming the new shape but for now the renderer culls every polygon in this configuration

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
    Matrix frustrum, projection, combined, allCombined, textureMap;

    BarycentricGpuRender_v4 renderer;//is a specific test
    JoclTexture texture;

    Map<Camera.Direction, Boolean> keys = new HashMap<>();

    PerformanceTimer t;

    PaintPad debugPad;

    double NEAR = 0.1, FAR = 30;

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

        JoclSetup setup = new JoclSetup(false);
        renderer = new BarycentricGpuRender_v4(screenWidth, screenHeight, setup);

        SingleTextureData textureDataHandler = new SingleTextureData(setup);
        texture = new JoclTexture("resources/Textures/default.png", textureDataHandler);

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
        wallPanelScene(relativePoints, textureRelativePoints);
    }

    private void singlePolygonScene(double[][] relativePoints, double[][] textureRelativePoints)
    {
        addPolygon(relativePoints, textureRelativePoints);
    }

    private void wallPanelScene(double[][] relativePoints, double[][] textureRelativePoints)
    {
        int width = 10;
        int height = 10;

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

        camera = new Camera(0,0, 2);
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

        allCombined = new Matrix(combined.matrixMultiply(projection));

        for (double[][] poly:polys)
        {
            double[][] clipPoints = new double[poly.length][3];
            for (int i=0;i<poly.length;i++) {
                clipPoints[i] = MatrixHelper.applyExplicitMatrix(allCombined, poly[i]);
            }
            clipPolys.add(clipPoints);
        }
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
            renderPolygon(clipPolys.get(i), polys.get(i), tAnchors.get(i));
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

    private void renderPolygon(double[][] cPolygon, double[][] rPolygon, double[][] tPolygon)
    {
        if (PolygonClipBoundsChecker.shouldCull(cPolygon)) return;

        //DEBUG START
//        debugPad.init();
//        Polygon poly = debugPad.createPolygon(new double[][]{
//                new double[]{0, 0},
//                new double[]{0, 1},
//                new double[]{1, 1},
//                new double[]{1, 0},
//                }, 50, 50);
//        debugPad.drawPolygon(poly, Color.BLACK, false);
//
//        poly = debugPad.createPolygon(tPolygon, 50, 50);
//        debugPad.drawPolygon(poly, Color.RED, false);
//
//        poly = debugPad.createPolygon(rPolygon, 50, 50);
//        debugPad.drawPolygon(poly, Color.BLUE, false);
//        debugPad.finish();
        //DEBUG END

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

