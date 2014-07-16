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
}
