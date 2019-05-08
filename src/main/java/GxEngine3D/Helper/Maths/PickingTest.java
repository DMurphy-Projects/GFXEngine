package GxEngine3D.Helper.Maths;

import GxEngine3D.Model.Matrix.Matrix;

//will become the basis of a "pickingCalc" if ever the need arises for one
public class PickingTest {

    //picks a static point for now, is the middle of the screen
    public static double[] pick3D(Matrix viewMatrix, Matrix projectionMatrix)
    {
        double[] ray = calculateMouseRay(new double[]{0, 0}, viewMatrix, projectionMatrix);

        return ray;
    }

    private static double[] calculateMouseRay(double[] point, Matrix viewMatrix, Matrix projectionMatrix) {
        double[] clipCoords = new double[]{point[0], point[1], -1, 1};//v4
        double[] eyeCoords = toEyeCoords(clipCoords, projectionMatrix);//v4
        double[] worldRay = toWorldCoords(eyeCoords, viewMatrix);//v3
        return worldRay;
    }

    //return v3, eye is v4
    private static double[] toWorldCoords(double[] eyeCoords, Matrix viewMatrix) {
        Matrix invertedView = new Matrix(viewMatrix.inverse_4x4());

        double[] rayWorld = invertedView.pointMultiply4(eyeCoords);//v4
        double[] mouseRay = new double[]{rayWorld[0], rayWorld[1], rayWorld[2]};//v3
        mouseRay = VectorCalc.norm(mouseRay);
        return mouseRay;
    }

    //return v4, clipCoord v4
    private static double[] toEyeCoords(double[] clipCoords, Matrix projectionMatrix) {
        Matrix invertedProjection = new Matrix(projectionMatrix.inverse_4x4());
        double[] eyeCoords = invertedProjection.pointMultiply4(clipCoords);//v4
        return new double[]{eyeCoords[0], eyeCoords[1], -1, 0};
    }
}
