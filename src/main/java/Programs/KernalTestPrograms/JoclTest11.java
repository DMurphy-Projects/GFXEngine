package Programs.KernalTestPrograms;

import GxEngine3D.Camera.Camera;
import GxEngine3D.Helper.Maths.FrustumMatrixHelper;
import GxEngine3D.Helper.Maths.MatrixHelper;
import GxEngine3D.Helper.PerformanceTimer;
import GxEngine3D.Model.Matrix.Matrix;
import TextureGraphics.GpuRendererIterateColor;
import TextureGraphics.Memory.Texture.JoclTexture;
import TextureGraphics.UpdateScene;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class JoclTest11 {
    public static void main(String args[])
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                new JoclTest11(480,480);
            }
        });
    }

    String VERBOSE = "v", SUCCINCT = "s";
    String debug;

    private int screenWidth = 0;
    private int screenHeight = 0;

    Camera camera;

    ArrayList<double[][]> relativePolys;
    ArrayList<double[][]> clipPolys;
    ArrayList<double[][]> screenPolys;

    double[][] translate, rotate, scale;
    Matrix frustrum, projection, combined, allCombined;

    UpdateScene renderer;//is a specific test

    PerformanceTimer t;

    double NEAR = 0.1, FAR = 30;

    public JoclTest11(int width, int height)
    {
        debug = VERBOSE;

        PerformanceTimer.Mode mode = debug == VERBOSE?
                PerformanceTimer.Mode.Nano:
                PerformanceTimer.Mode.Total;

        t = new PerformanceTimer(mode);

        screenWidth = width;
        screenHeight = height;

        renderer = new UpdateScene(screenWidth, screenHeight);

        initScene();
        addPolygons();

        setupMatrices();

        t.time();
        updateSceneCpu();
        t.time();
        updateSceneGpu();
        t.time();

        t.printNextTime("CPU");
        t.printNextTime("GPU - Initial");
    }

    private void updateSceneCpu()
    {
        updateScene();
    }

    private void updateSceneGpu()
    {
        renderer.setup();
        renderer.prepare(relativePolys);
        renderer.setMatrix(allCombined.flatten());

        for (double[][] polygon: relativePolys)
        {
            renderer.addPolygon(polygon);
        }

        renderer.enqueue();
        renderer.update();
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

    private void singlePolygonScene(double[][] relativePoints)
    {
        addPolygon(relativePoints);
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

    private void intersectionScene(double[][] relativePoints)
    {
        addPolygon(relativePoints);

        relativePoints = Arrays.copyOfRange(relativePoints, 0, relativePoints.length);

        double[][] rotate = MatrixHelper.setupFullRotation(0, Math.PI/2, 0);
        applyMatrix(relativePoints, new Matrix(rotate));

        addPolygon(relativePoints);
    }

    private void applyMatrix(double[][] relativePoints, Matrix combined)
    {
        for (int i=0;i<relativePoints.length;i++) {
            relativePoints[i] = MatrixHelper.applyImplicitMatrix(combined, relativePoints[i]);
        }
    }

    private void addPolygon(double[][] polygon)
    {
        relativePolys.add(polygon);
    }

    private void initScene()
    {
        relativePolys = new ArrayList<>();
        clipPolys = new ArrayList<>();
        screenPolys = new ArrayList<>();

        camera = new Camera(5,5, 10);
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

        for (double[][] poly:relativePolys)
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
