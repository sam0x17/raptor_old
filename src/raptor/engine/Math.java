/**
 * Contains some math routines used throughout the RAPTOR system
 */
package raptor.engine;

import org.jblas.*;

import static raptor.engine.Image.*;

/**
 * @author Sam Kelly
 *
 */
public final class Math
{
/*
func get_matrix_distance(M1, M2)
	var E = joint eigenvalues of M1 and M2
	var sum = 0
	for i = 0 to size(E)
		sum += ln^2(E_i)
	end
	return sqrt(sum)
end
 */
	public static double matrix_difference_metric(DoubleMatrix A, DoubleMatrix B)
	{
		DoubleMatrix E = Eigen.symmetricGeneralizedEigenvalues(A, B);
		double sum = 0.0;
		for(double Ei : E.data)
		{
			double ln = java.lang.Math.log(Ei);
			sum += ln * ln;
		}
		return java.lang.Math.sqrt(sum);
	}
	
	public static String matrix2String(DoubleMatrix matrix)
	{
		double M[][] = matrix.toArray2();
		String st = "";
		for(int r = 0; r < M.length; r++)
		{
			st += "[";
			for(int c = 0; c < M[0].length; c++)
			{
				st += M[r][c];
				if(c + 1 < M[0].length) st += ",\t";
			}
			st += "]\n";
		}
		return st;
	}
	
	public static void printMatrix(DoubleMatrix matrix)
	{
		printMatrix(matrix, null);
	}
	
	public static void printMatrix(DoubleMatrix matrix, String label)
	{
		double M[][] = matrix.toArray2();
		for(int r = 0; r < M.length; r++)
		{
			System.out.print("[");
			for(int c = 0; c < M[0].length; c++)
			{
				System.out.print(M[r][c]);
				if(c + 1 < M[0].length)
					System.out.print(",\t");
			}
			System.out.println("]");
		}
		System.out.println();
	}

	public static DoubleMatrix imageAs2DPointCloud(PixelGrid grid, double threshold)
	{
		int num_ones = 0;
		for(int y = 0; y < grid.pixels.length; y++)
			for(int x = 0; x < grid.pixels[0].length; x++)
				if(grid.pixels[y][x] >= threshold) num_ones++;
		double vals[][] = new double[2][num_ones];
		for(int y = 0, k = 0; y < grid.pixels.length; y++)
		{
			for(int x = 0; x < grid.pixels[0].length; x++)
			{
				if(grid.pixels[y][x] < threshold) continue;
				vals[0][k] = x;
				vals[1][k] = y;
				k++;
			}
		}
		return new DoubleMatrix(vals);
	}
	
	public static DoubleMatrix getColMean(DoubleMatrix data)
	{
		DoubleMatrix mean = new DoubleMatrix(data.rows, 1);
		for(int i = 0; i < data.columns; i++)
			mean.addi(data.getColumn(i));
		DoubleMatrix m = new DoubleMatrix(new double[data.rows][1]);
		for(int i = 0; i < m.rows; i++)
			m.put(i, 0, data.columns);
		mean.divi(m);
		return mean;
	}
	
	public static DoubleMatrix getCovarianceMatrix(DoubleMatrix data)
	{
		DoubleMatrix result = new DoubleMatrix(data.rows, data.rows);
		DoubleMatrix mean = getColMean(data);
		for(int i = 0; i < data.columns; i++)
		{
			DoubleMatrix xi = data.getColumn(i);
			xi.subi(mean);
			result.addi(xi.mmul(xi.transpose()));
		}
		result.divi(data.columns - 1);
		return result;
	}
	
	public static class BasicStat
	{
		private int numSamples = 0;
		private double sum = 0.0;
		private double min = Double.MAX_VALUE;
		private double max = Double.MIN_VALUE;
		
		public void addSample(double sample)
		{
			sum += sample;
			numSamples++;
			if(sample < min)
				min = sample;
			else if(sample > max)
				max = sample;
		}
		
		public double getAverage()
		{
			return sum / (double)numSamples;
		}
		
		public double getSum()
		{
			return sum;
		}
		
		public double getMin()
		{
			return min;
		}
		
		public double getMax()
		{
			return max;
		}
		
		public void prettyPrint(String name)
		{
			System.out.println(name + " avg: " + getAverage());
			System.out.println(name + " min: " + getMin());
			System.out.println(name + " max: " + getMax());
			System.out.println(name + " sum: " + getSum());
			System.out.println();
		}
	}
	
	public abstract static class BasicObserverStat<T> extends BasicStat
	{
		public abstract void update(T parent);
	}
}
