package Distance;

import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.Instance;
import net.sf.javaml.distance.*;
import net.sf.javaml.tools.DatasetTools;

/**
 * Created by Sparsh Bansal on 4/20/2016.
 */
public class NormalizedEuclideanDistance extends net.sf.javaml.distance.EuclideanDistance {
    private static final long serialVersionUID = -6489071802740149683L;
    private Dataset data;

    private Instance min = null;
    private Instance max = null;

    public NormalizedEuclideanDistance(Dataset data) {
        this.data = data;
        min = DatasetTools.minAttributes(this.data);
        max = DatasetTools.maxAttributes(this.data);
    }

    public double measure(Instance i, Instance j) {
        Instance normI = this.normalizeMidrange(0.5D, 1.0D, min, max, i);
        Instance normJ = this.normalizeMidrange(0.5D, 1.0D, min, max, j);
        return super.calculateDistance(normI, normJ) / Math.sqrt((double)i.noAttributes());
    }

    private Instance normalizeMidrange(double normalMiddle, double normalRange, Instance min, Instance max, Instance instance) {
        double[] out = new double[instance.noAttributes()];

        for(int i = 0; i < out.length; ++i) {
            double range = Math.abs(max.value(i) - min.value(i));
            double middle = Math.abs(max.value(i) + min.value(i)) / 2.0D;
            out[i] = (instance.value(i) - middle) / range * normalRange + normalMiddle;
        }

        return new DenseInstance(out, instance);
    }

}
