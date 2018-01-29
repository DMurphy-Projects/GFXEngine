package GxEngine3D.Ordering;

/**
 * Created by Dean on 31/12/16.
 */
public abstract class BaseOrdering implements IOrderStrategy {
    protected int[] sortIndex(double[] k) {
        int[] newOrder = new int[k.length];

        for (int i = 0; i < k.length; i++) {
            newOrder[i] = i;
        }
        return sortIndex(k, newOrder);
    }

    // gives a new indice order for k without sorting k - faster
    protected int[] sortIndex(double[] k, int[] order) {
        double temp;
        int tempr;
        boolean sorted = false;
        while (!sorted) {
            sorted = true;
            for (int b = 0; b < k.length - 1; b++) {
                if (k[b] < k[b + 1]) {
                    sorted = false;
                    temp = k[b];
                    tempr = order[b];
                    order[b] = order[b + 1];
                    k[b] = k[b + 1];

                    order[b + 1] = tempr;
                    k[b + 1] = temp;
                    break;
                }
            }
        }
        return order;
    }
}
