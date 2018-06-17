package GxEngine3D.Ordering;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dean on 31/12/16.
 */
public abstract class BaseOrdering implements IOrderStrategy {
    //technically doesn't need to be great as it does not work
    //only exists for historical reasons
    //NOTE: uses around 42%
    protected List<Integer> sortIndex(double[] k) {
        int[] newOrder = new int[k.length];

        for (int i = 0; i < k.length; i++) {
            newOrder[i] = i;
        }
        ArrayList<Integer> order = new ArrayList<>();
        for (int i:sortIndex(k, newOrder))
        {
            order.add(i);
        }
        return order;
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
