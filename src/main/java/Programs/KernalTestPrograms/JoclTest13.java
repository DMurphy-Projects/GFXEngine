package Programs.KernalTestPrograms;

import Games.Ant.AntGame;
import Games.IGameScreen;
import GxEngine3D.Camera.Camera;
import GxEngine3D.Helper.Maths.FrustumMatrixHelper;
import GxEngine3D.Helper.Maths.MatrixHelper;
import GxEngine3D.Helper.PerformanceTimer;
import GxEngine3D.Model.Matrix.Matrix;
import TextureGraphics.*;
import TextureGraphics.FragmentRender.BackgroundFragment;
import TextureGraphics.FragmentRender.SolidColorFragment;
import TextureGraphics.FragmentRender.TextureFragmentRefPoint;
import TextureGraphics.Intermediate.GpuIntermediateRefPoint;
import TextureGraphics.Memory.BufferHelper;
import TextureGraphics.Memory.JoclMemoryMethods;
import TextureGraphics.Memory.PixelOutputHandler;
import TextureGraphics.Memory.Texture.*;
import TextureGraphics.Memory.Texture.Enumeration.RenderType;
import TextureGraphics.Memory.Texture.Enumeration.TextureType;
import TextureGraphics.UpdateScene.UpdateCulling;
import TextureGraphics.UpdateScene.UpdateRefPoint;
import org.jocl.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.jocl.CL.CL_MEM_READ_ONLY;

//this method uses a 3 stage approach:
//  1: update scene via update kernel
//  2: calculate map information, allows to determine which triangle is being rendered at each pixel
//  3: apply render fragments, apply some coloring based on the information calculated in step 2
public class JoclTest13 {

    private BufferedImage image;
    private JFrame frame;
    private JPanel imageComponent;

    private int screenWidth = 0;
    private int screenHeight = 0;

    Camera camera;

    ArrayList<double[][]> relativePolys, textureRelativePolys;

    double[][] translate, rotate, scale;
    Matrix frustrum, projection, combined, allCombined, textureMap;

    UpdateRefPoint pointUpdater;
    UpdateCulling cullingUpdater;

    GpuIntermediateRefPoint intermediate;

    SolidColorFragment solidFragment;
    TextureFragmentRefPoint textureFragment;
    BackgroundFragment backgroundFragment;

    double NEAR = 0.1, FAR = 30;

    Map<Camera.Direction, Boolean> keys = new HashMap<>();

    int[] colorArray = new int[]{
            Color.RED.darker().getRGB(),
            Color.BLUE.darker().getRGB(),
            Color.GREEN.darker().getRGB(),
    };

    String VERBOSE = "v", SUCCINCT = "s";
    String debug;
    PerformanceTimer t;

    PixelOutputHandler outputHandler;

    boolean update = false;

    public static void main(String[] arg)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                new JoclTest13(480,480);
            }
        });
    }

    Pointer metaDataPointer, textureMetaPointer;

    String[] texturePaths = {
            "resources/Textures/default.png",
            "resources/Textures/bluePixel.png",
            "resources/Textures/blank.png",
    };

    ITexture[] textures;
    MultiTextureData textureDataHandler;

    int[] textureTypeData, metaData, textureMetaData;

    final int ANT_TEXTURE = 2;

    public JoclTest13(int width, int height)
    {
        debug = VERBOSE;

        PerformanceTimer.Mode mode = debug == VERBOSE?
                PerformanceTimer.Mode.Nano:
                PerformanceTimer.Mode.Total;

        t = new PerformanceTimer(mode);

        screenWidth = width;
        screenHeight = height;

        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        JoclSetup setup = new JoclSetup(true);

        pointUpdater = new UpdateRefPoint(screenWidth, screenHeight, setup);
        cullingUpdater = new UpdateCulling(screenWidth, screenHeight, setup);

        intermediate = new GpuIntermediateRefPoint(screenWidth, screenHeight, setup);

        solidFragment = new SolidColorFragment(screenWidth, screenHeight, setup);
        textureFragment = new TextureFragmentRefPoint(screenWidth, screenHeight, setup);
        backgroundFragment = new BackgroundFragment(screenWidth, screenHeight, setup);

        initScene();
        addPolygons();

        setupMatrices();

        updateRelativePolygons();

        //init meta arrays
        metaData = new int[relativePolys.size()];
        textureMetaData = new int[relativePolys.size()];
        textureTypeData = new int[texturePaths.length];

        textureFragment.prepareTextureRelative(textureRelativePolys);

        //----------META
        setupMetaData(setup);
        solidFragment.setupMetaInfoArray(metaDataPointer);
        textureFragment.setupMetaInfoArray(metaDataPointer);
        textureFragment.setupTextureMetaArray(textureMetaPointer);
        //----------META

        outputHandler = new PixelOutputHandler(setup);
        outputHandler.setup(width, height);

        solidFragment.setupPixelOut(outputHandler.getPixelOut());
        textureFragment.setupPixelOut(outputHandler.getPixelOut());
        backgroundFragment.setupPixelOut(outputHandler.getPixelOut());

        solidFragment.prepareColorArray(relativePolys.size(), colorArray);

        //----------TEXTURES
        prepareTextures(setup);
        MemoryDataPackage _package = textureDataHandler.getClTextureData();
        textureFragment.setupTextureDataArray(Pointer.to(_package.data));
        textureFragment.setupTextureInfoArray(textureDataHandler.getClTextureInfoData());
        //----------TEXTURES

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

        setupRenderLoop();
        setupGame((JoclDynamicTexture)textures[ANT_TEXTURE]);

        invalidate();
    }

    private void setupGameMeta(int polygonNumber)
    {
        metaData[polygonNumber] = RenderType.TEXTURE;
        textureMetaData[polygonNumber] = ANT_TEXTURE;
        textureTypeData[ANT_TEXTURE] = TextureType.DYNAMIC;
    }

    private void setupGame(IGameScreen screen)
    {
        AntGame game = new AntGame(screen);

        game.addAction(Color.WHITE, true);
        game.addAction(Color.BLACK, false);
        game.addAction(Color.GREEN.darker(), false);
        game.addAction(Color.GREEN, true);

        //game update loop
        new Thread(new Runnable() {
            @Override
            public void run() {

                while (true)
                {
                    game.tick();

                    //update the texture in the texture fragment
                    MemoryDataPackage _package = textureDataHandler.getClTextureData();
                    textureFragment.setupTextureDataArray(Pointer.to(_package.data));

                    invalidate();
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void setupRenderLoop()
    {
        //render loop
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true)
                {
                    if (update)
                    {
                        update = false;
                        updateScreen();
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void invalidate()
    {
        update = true;
    }

    private void setupMetaData(JoclSetup setup)
    {
        setupGameMeta(1);//ant game

        //flag the first polygon as a texture
        metaData[0] = RenderType.TEXTURE;

        cl_mem m1 = JoclMemoryMethods.write(setup.getContext(), setup.getCommandQueue(),
                BufferHelper.createBuffer(metaData), CL_MEM_READ_ONLY);
        metaDataPointer = Pointer.to(m1);

        //NOTE: index corresponds to the texture array
        textureMetaData[0] = 0;//set the first polygon to the first texture, bad example since all start with 0 anyway...

        cl_mem m2 = JoclMemoryMethods.write(setup.getContext(), setup.getCommandQueue(),
                BufferHelper.createBuffer(textureMetaData), CL_MEM_READ_ONLY);
        textureMetaPointer = Pointer.to(m2);
    }

    private void prepareTextures(JoclSetup setup)
    {
        BufferedImage[] images = new BufferedImage[texturePaths.length];

        int totalSize = 0;
        for (int i=0;i<images.length;i++)
        {
            String path = texturePaths[i];
            try {
                BufferedImage image = ImageIO.read(new File(getClass().getClassLoader().getResource(path).getFile()));
                images[i] = image;

                totalSize += images[i].getWidth() * images[i].getHeight();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        textureDataHandler = new MultiTextureData(setup);
        textureDataHandler.init(totalSize, images.length);

        textures = new JoclTexture[images.length];

        for (int i=0;i<images.length;i++)
        {
            switch (textureTypeData[i])
            {
                case TextureType.REGULAR:
                    textures[i] = new JoclTexture(images[i], textureDataHandler);
                    break;
                case TextureType.DYNAMIC:
                    textures[i] = new JoclDynamicTexture(images[i], textureDataHandler);
                    break;
            }
        }
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
                        invalidate();
                        break;
                    case 'i':
                        break;
                    case 'l':
                        invalidate();
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
                invalidate();
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
                invalidate();
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
                int[] center = new int[]{
                        p.x + (screenWidth/2),
                        p.y + (screenHeight/2)
                };

                camera.MouseMovement(center[0] - e.getXOnScreen(), center[1] - e.getYOnScreen());
                centreMouse();

                invalidate();
            }
        });
    }

    public void centreMouse() {
        try {
            Robot r = new Robot();

            Point loc = imageComponent.getLocationOnScreen();

            r.mouseMove(loc.x + (screenWidth/2), loc.y + (screenHeight/2));
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    private void updateRelativePolygons()
    {
        ArrayList<int[]> polygonIndices = new ArrayList<>();
        ArrayList<double[]> polygonPoints = new ArrayList<>();

        int sizeOffset = 0;
        for (double[][] polygon: relativePolys)
        {
            //create local indexing, this will be done by the shape itself in future
            int[] localPolygonIndex = new int[polygon.length * 3];
            int indexCount = 0;
            for (double[] point: polygon)
            {
                for (double _: point) {
                    localPolygonIndex[indexCount] = indexCount++;
                }

                polygonPoints.add(point);
            }
            //convert local index to global index
            for (int i=0;i<localPolygonIndex.length;i++)
            {
                localPolygonIndex[i] += sizeOffset;
            }
            polygonIndices.add(localPolygonIndex);
            sizeOffset += localPolygonIndex.length;
        }

        pointUpdater.prepare(polygonPoints);
        cullingUpdater.prepare(polygonPoints, polygonIndices);
    }

    private void updateSceneGpu()
    {
        createProjectionMatrix();

        allCombined = new Matrix(combined.matrixMultiply(projection));

        updatePoints();
        updateCulling();
    }

    private void updateCulling()
    {
        cullingUpdater.setup();

        cullingUpdater.setupClipArray(pointUpdater.getClipPolygonArray());

        cullingUpdater.enqueue();

        cullingUpdater.readData();
    }

    private void updatePoints()
    {
        pointUpdater.setup();

        pointUpdater.setMatrix(allCombined.flatten());

        pointUpdater.enqueue();

        pointUpdater.readData();
    }

    private void updateIntermediate()
    {
        intermediate.setup();

        intermediate.setupPointArray(pointUpdater.getScreenPolygonArray());
        intermediate.setupPolygonStartArray(cullingUpdater.getPolygonStartArray());
        intermediate.setupPolygonArray(cullingUpdater.getPolygonArray());

        pointUpdater.waitOnReadTasks();

        intermediate.prepare(cullingUpdater.getIndexArrayData());
        intermediate.setupBoundBox(pointUpdater.getScreenPolygonBuffer(), cullingUpdater.getPolygonStartData());

        if(intermediate.hasWorkToDo())
        {
            intermediate.setupOutputMemory();
        }

        intermediate.enqueueTasks(pointUpdater.getTask());
    }

    private void updateFragments()
    {
        updateSolidFragment();
        updateTextureFragment();
        updateBackgroundFragment();
    }

    private void updateBackgroundFragment()
    {
        backgroundFragment.setup();

        backgroundFragment.setupOutMap(intermediate.getOutMap());

        backgroundFragment.enqueueTasks(intermediate.getTask());
    }

    private void updateSolidFragment()
    {
        solidFragment.setup();

        solidFragment.setupIndexArray(intermediate.getIndexArray());
        solidFragment.setupOutMap(intermediate.getOutMap());

        solidFragment.enqueueTasks(intermediate.getTask());
    }

    private void updateTextureFragment()
    {
        textureFragment.setup();

        textureFragment.setupIndexArray(intermediate.getIndexArray());
        textureFragment.setupOutMap(intermediate.getOutMap());
        textureFragment.setupZMap(intermediate.getZMap());
        textureFragment.setupRelativeArray(pointUpdater.getRelativePolygonArray());
        textureFragment.setupPolygonArray(cullingUpdater.getPolygonArray());
        textureFragment.setupPolygonStartArray(cullingUpdater.getPolygonStartArray());

        double[] flatInverse = new Matrix(allCombined.inverse_4x4()).flatten();
        textureFragment.setInverseMatrix(flatInverse);

        textureFragment.enqueueTasks(intermediate.getTask());
    }

    private void readDataFromFragments(int[] data)
    {
        solidFragment.waitOnFinishing();
        textureFragment.waitOnFinishing();

        outputHandler.read(data);
        imageComponent.repaint();
    }

    private void updateScreen()
    {
        t.time();
        updateSceneGpu();

        DataBufferInt dataBuffer = (DataBufferInt) image.getRaster().getDataBuffer();
        int data[] = dataBuffer.getData();

        t.time();
        updateIntermediate();
        t.time();
        if (intermediate.hasWorkToDo())
        {
            updateFragments();
        }
        else
        {
            //since the intermediate had no work to do, there will be no outMap generated
            //make it create a blank one and setup to release once background fragment is finished with it
            intermediate.setupBlankOutMap(backgroundFragment.getTask());
            updateBackgroundFragment();
        }
        t.time();

        readDataFromFragments(data);
        t.time();

        pointUpdater.finish();
        intermediate.finish();
        solidFragment.finish();

        if (debug == SUCCINCT)
        {
            t.printNextTime("Total Took");
        }
        else if (debug == VERBOSE)
        {
            t.printNextTime("Update Scene");
            t.printNextTime("Update Intermediate");
            t.printNextTime("Update Fragments");
            t.printNextTime("Read From Fragments");
            System.out.println();
        }

        t.reset();
    }

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

    private void addPolygon(double[][] polygon, double[][] textureAnchor)
    {
        relativePolys.add(polygon);
        textureRelativePolys.add(textureAnchor);
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
        textureRelativePolys = new ArrayList<>();

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
