package DBSCAN;

import net.sf.javaml.core.Instance;
import net.sf.javaml.distance.DistanceMeasure;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Created by Sparsh Bansal on 4/20/2016.
 */
abstract class AbstractDensityBasedClustering {
    DistanceMeasure dm;
    Vector<DataObject> dataset = null;

    AbstractDensityBasedClustering() {
    }

    List<DataObject> epsilonRangeQuery(double epsilon, AbstractDensityBasedClustering.DataObject inst) {
        ArrayList epsilonRange_List = new ArrayList();

        for(int i = 0; i < this.dataset.size(); ++i) {
            AbstractDensityBasedClustering.DataObject tmp = (AbstractDensityBasedClustering.DataObject)this.dataset.get(i);
            double distance = this.dm.measure(tmp.instance, inst.instance);
            if(distance < epsilon) {
                epsilonRange_List.add(tmp);
            }
        }

        return epsilonRange_List;
    }

    class DataObject {
        int clusterIndex = -1;
        double c_dist;
        double r_dist;
        boolean processed = false;
        static final int UNCLASSIFIED = -1;
        static final int UNDEFINED = 2147483647;
        static final int NOISE = -2;
        Instance instance;

        public DataObject(Instance inst) {
            this.instance = inst;
        }

        public boolean equals(Object obj) {
            AbstractDensityBasedClustering.DataObject tmp = (AbstractDensityBasedClustering.DataObject)obj;
            return tmp.instance.equals(this.instance);
        }

        public int hashCode() {
            return this.instance.hashCode();
        }

        public String getKey() {
            return this.instance.toString();
        }
    }
}
