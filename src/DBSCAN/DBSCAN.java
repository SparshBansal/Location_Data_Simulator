package DBSCAN;

import net.sf.javaml.clustering.Clusterer;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.distance.DistanceMeasure;
import net.sf.javaml.distance.NormalizedEuclideanDistance;

import java.util.*;

/**
 * Created by Sparsh Bansal on 4/20/2016.
 */
public class DBSCAN extends AbstractDensityBasedClustering implements Clusterer {
    private double epsilon;
    private int minPoints;
    private int clusterID;
    private Dataset originalData;

    public DBSCAN() {
        this(0.1D, 6);
    }

    public DBSCAN(double epsilon, int minPoints) {
        this(epsilon, minPoints, (DistanceMeasure) null);
    }

    public DBSCAN(double epsilon, int minPoints, DistanceMeasure dm) {
        this.originalData = null;
        this.dm = dm;
        this.epsilon = epsilon;
        this.minPoints = minPoints;
    }

    private boolean expandCluster(AbstractDensityBasedClustering.DataObject dataObject) {
        HashSet usedSeeds = new HashSet();
        List seedList = this.epsilonRangeQuery(this.epsilon, dataObject);
        usedSeeds.addAll(seedList);
        if (seedList.size() < this.minPoints) {
            dataObject.clusterIndex = -2;
            return false;
        } else {
            for (int seedListDataObject = 0; seedListDataObject < seedList.size(); ++seedListDataObject) {
                AbstractDensityBasedClustering.DataObject seedListDataObject_Neighbourhood = (AbstractDensityBasedClustering.DataObject) seedList.get(seedListDataObject);
                seedListDataObject_Neighbourhood.clusterIndex = this.clusterID;
                if (seedListDataObject_Neighbourhood.equals(dataObject)) {
                    seedList.remove(seedListDataObject);
                    --seedListDataObject;
                }
            }

            for (; seedList.size() > 0; seedList.remove(0)) {
                AbstractDensityBasedClustering.DataObject var8 = (AbstractDensityBasedClustering.DataObject) seedList.get(0);
                List var9 = this.epsilonRangeQuery(this.epsilon, var8);
                if (var9.size() >= this.minPoints) {
                    for (int i = 0; i < var9.size(); ++i) {
                        AbstractDensityBasedClustering.DataObject p = (AbstractDensityBasedClustering.DataObject) var9.get(i);
                        if ((p.clusterIndex == -1 || p.clusterIndex == -2) && p.clusterIndex == -1 && !usedSeeds.contains(p)) {
                            seedList.add(p);
                            usedSeeds.add(p);
                        }

                        p.clusterIndex = this.clusterID;
                    }
                }
            }

            return true;
        }
    }

    public Dataset[] cluster(Dataset data) {
        this.originalData = data;
        if (this.dm == null) {
            this.dm = new NormalizedEuclideanDistance(this.originalData);
        }

        this.clusterID = 0;
        this.dataset = new Vector();

        for (int output = 0; output < data.size(); ++output) {
            this.dataset.add(new AbstractDensityBasedClustering.DataObject(data.instance(output)));
        }

        Collections.shuffle(this.dataset);
        ArrayList var5 = new ArrayList();
        Iterator i$ = this.dataset.iterator();

        while (i$.hasNext()) {
            AbstractDensityBasedClustering.DataObject dataObject = (AbstractDensityBasedClustering.DataObject) i$.next();
            if (dataObject.clusterIndex == -1 && this.expandCluster(dataObject)) {
                var5.add(this.extract(this.clusterID));
                ++this.clusterID;
            }
        }

        return (Dataset[]) var5.toArray(new Dataset[0]);
    }

    private Dataset extract(int clusterID) {
        DefaultDataset cluster = new DefaultDataset();
        Iterator i$ = this.dataset.iterator();

        while (i$.hasNext()) {
            AbstractDensityBasedClustering.DataObject dataObject = (AbstractDensityBasedClustering.DataObject) i$.next();
            if (dataObject.clusterIndex == clusterID) {
                cluster.add(dataObject.instance);
            }
        }

        return cluster;
    }
}
