package Shapes;

/**
 * Created by Dean on 28/12/16.

 NOTE:
 defines an object that can be manipulated in the following ways:
    -scaling
    -rotation
    -translation
 absolute refers to setting to a specific value
 scale should be treated as multiplicative whereas rotate/translate should be additive
 */

public interface IManipulable {
    void scale(double x, double y, double z);
    void absoluteScale(double x, double y, double z);

    void rotate(double pitch, double yaw, double roll);
    void absoluteRotate(double pitch, double yaw, double roll);

    void translate(double x, double y, double z);
    void absoluteTranslate(double x, double y, double z);


}
