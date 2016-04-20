package DBSCAN;

import net.sf.javaml.clustering.Clusterer;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.distance.DistanceMeasure;
import net.sf.javaml.distance.NormalizedEuclideanDistance;
import net.sf.javaml.tools.ListTools;

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
        List<DataObject> epsNeighbourhood = this.epsilonRangeQuery(this.epsilon, dataObject);

        if (epsNeighbourhood.size() < minPoints) {
            dataObject.clusterIndex = DataObject.NOISE;
            return false;
        }


        // All pts in epsNeighbourhood are in the same cluster
        Iterator<DataObject> iterator = epsNeighbourhood.iterator();
        while (iterator.hasNext()) {
            DataObject currentPoint = iterator.next();
            currentPoint.clusterIndex = this.clusterID;
        }

        epsNeighbourhood.remove(dataObject);

        Queue<DataObject> queue = new LinkedList<>();
        queue.addAll(epsNeighbourhood);

        while (!queue.isEmpty()) {
            DataObject neighbouringPoint = queue.remove();
            List<DataObject> neighbourhood = this.epsilonRangeQuery(this.epsilon, neighbouringPoint);

            if (neighbourhood.size() < minPoints)
                continue;
            else {
                Iterator<DataObject> iterator1 = neighbourhood.iterator();
                while (iterator1.hasNext()) {
                    DataObject newObject = iterator1.next();
                    if (newObject.clusterIndex == DataObject.NOISE || newObject.clusterIndex < 0) {
                        newObject.clusterIndex = this.clusterID;
                        queue.add(newObject);
                    }
                }
            }
        }

        return true;
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
        Iterator datasetIterator = dataset.iterator();

        while (datasetIterator.hasNext()) {
            DataObject currentPoint = (DataObject) datasetIterator.next();
            if (currentPoint.clusterIndex == DataObject.NOISE || currentPoint.clusterIndex >= 0)
                continue;
            else {
                if (expandCluster(currentPoint)) {
                    this.clusterID++;
                    System.out.println("Cluster : " + clusterID);
                }

            }
        }

        return extractClusters();
    }

    private Dataset[] extractClusters() {
        Dataset[] clusters = new Dataset[this.clusterID];
        for (int i = 0; i < this.clusterID; i++)
            clusters[i] = new DefaultDataset();
        Iterator<DataObject> iterator = this.dataset.iterator();

        while (iterator.hasNext()) {
            DataObject currentPoint = iterator.next();
            if (currentPoint.clusterIndex >= 0) {
                clusters[currentPoint.clusterIndex].add(currentPoint.instance);
            }

        }
        return clusters;
    }
}
