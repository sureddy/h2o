package samples;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Random;

import water.*;
import water.fvec.*;
import water.util.Utils;

/**
 * Simplified version of H2O k-means algorithm for better readability.
 */
public class MinimalKMeans extends Job {
  public static void main(String[] args) throws Exception {
    CloudProcess.launch(2, MinimalKMeans.class);
  }

  @Override protected void exec() {
    // Load and parse a file. Data is distributed to other nodes in a round-robin way
    Key file = NFSFileVec.make(new File("../lib/resources/datasets/gaussian.csv"));
    Frame frame = ParseDataset2.parse(Key.make("test"), new Key[] { file });

    // Optionally create a frame with less columns, e.g. skip first
    frame = new Frame(Utils.remove(frame._names, 0), Utils.remove(frame.vecs(), 0));

    // Create k clusters as arrays of doubles
    int k = 7;
    double[][] clusters = new double[k][frame.vecs().length];

    // Initialize first cluster to random row
    Random rand = new Random();
    for( int cluster = 0; cluster < clusters.length; cluster++ ) {
      long row = Math.max(0, (long) (rand.nextDouble() * frame.vecs().length) - 1);
      for( int i = 0; i < frame.vecs().length; i++ ) {
        Vec v = frame.vecs()[i];
        clusters[cluster][i] = v.at(row);
      }
    }

    // Iterate over the dataset and show error for each step
    for( int i = 0; i < 10; i++ ) {
      KMeans task = new KMeans();
      task._clusters = clusters;
      task.doAll(frame);

      for( int c = 0; c < clusters.length; c++ ) {
        if( task._counts[c] > 0 ) {
          for( int v = 0; v < frame.vecs().length; v++ ) {
            double value = task._sums[c][v] / task._counts[c];
            clusters[c][v] = value;
          }
        }
      }
      System.out.println("Error is " + task._error);
    }

    System.out.println("Clusters:");
    DecimalFormat df = new DecimalFormat("#.00");
    for( int c = 0; c < clusters.length; c++ ) {
      for( int v = 0; v < frame.vecs().length; v++ )
        System.out.print(df.format(clusters[c][v]) + ", ");
      System.out.println("");
    }
  }

  /**
   * For more complex tasks like this one, it is useful to marks fields that are provided by the
   * caller (IN), and fields generated by the task (OUT). IN fields can then be set to null when the
   * task is done using them, so that they do not get serialized back to the caller.
   */
  public static class KMeans extends MRTask2<KMeans> {
    double[][] _clusters; // IN:  Centroids/clusters

    double[][] _sums;     // OUT: Sum of features in each cluster
    int[] _counts;        // OUT: Count of rows in cluster
    double _error;        // OUT: Total sqr distance

    @Override public void map(Chunk[] chunks) {
      _sums = new double[_clusters.length][chunks.length];
      _counts = new int[_clusters.length];

      // Find nearest cluster for each row
      for( int row = 0; row < chunks[0]._len; row++ ) {
        int nearest = -1;
        double minSqr = Double.MAX_VALUE;
        for( int cluster = 0; cluster < _clusters.length; cluster++ ) {
          double sqr = 0;           // Sum of dimensional distances
          for( int column = 0; column < chunks.length; column++ ) {
            double delta = chunks[column].at0(row) - _clusters[cluster][column];
            sqr += delta * delta;
          }
          if( sqr < minSqr ) {
            nearest = cluster;
            minSqr = sqr;
          }
        }
        _error += minSqr;

        // Add values and increment counter for chosen cluster
        for( int column = 0; column < chunks.length; column++ )
          _sums[nearest][column] += chunks[column].at0(row);
        _counts[nearest]++;
      }
      _clusters = null;
    }

    @Override public void reduce(KMeans task) {
      for( int cluster = 0; cluster < _counts.length; cluster++ ) {
        for( int column = 0; column < _sums[0].length; column++ )
          _sums[cluster][column] += task._sums[cluster][column];
        _counts[cluster] += task._counts[cluster];
      }
      _error += task._error;
    }
  }
}
