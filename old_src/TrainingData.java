import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

/**
 * Class for representing training data for use with a BackpropNetwork
 * 
 * @author Sam Kelly
 *
 */

public class TrainingData
{
	public double input[][];
	public double output[][];
	
	private static Random rand = new Random();
	
	public TrainingData(double input[][], double output[][])
	{
		if(input == null && output == null) return;
		this.input = input.clone();
		this.output = output.clone();
	}
	
	/**
	 * Given an array of doubles such as arr[][], swaps arr[a][col]
	 * with arr[b][col]
	 * 
	 * @param arr the array in which the swap will occur
	 * @param col the column in which the swap will occur
	 * @param a the index of element a
	 * @param b the index of element b
	 */
	private static void doubleswap(double arr[][], int col, int a, int b)
	{
		double tmp = arr[a][col];
		arr[a][col] = arr[b][col];
		arr[b][col] = tmp;
	}
	
	// about to be re-written
	/*
	public static TrainingData generateRandomData(int num_samples)
	{
		double input_data[][] = new double[num_samples][1];
		double output_data[][] = new double[num_samples][1];
		double spread = (0.9 - 0.1) / (double)(num_samples);
		for(int i = 0; i < num_samples; i++)
		{
				input_data[i][0] = 0.1 + spread * (double)i;
				output_data[i][0] = input_data[i][0];
		}
		
		// randomly sort the output_data array
		for(int i = 0; i < num_samples; i++)
		{
			doubleswap(output_data, 0, i, rand.nextInt(num_samples));
		}
		
		return new TrainingData(input_data, output_data);
	}
	*/

	
	public String toString()
	{
		String ret = "";
		for(int i = 0; i < input.length; i++)
		{
			for(int j = 0; j < input[i].length; j++)
			{
				ret += input[i][j] + " ";
			}
			ret += ": ";
			for(int j = 0; j < output[i].length; j++)
			{
				ret += output[i][j] + " ";
			}
			ret += "\n";
		}
		return ret;
	}
	
	public static boolean outputIsCorrect(double actual_output[], double desired_output[])
	{
		for(int i = 0; i < actual_output.length; i++)
		{
			if((int)Math.round(actual_output[i]) != (int)Math.round(desired_output[i])) return false;
		}
		return true;
	}
	
	public static int getNumErrors(double actual_output[], double desired_output[])
	{
		int num_wrong = 0;
		for(int i = 0; i < actual_output.length; i++)
		{
			if((int)Math.round(actual_output[i]) != (int)Math.round(desired_output[i]))
			{
				num_wrong++;
			}
		}
		return num_wrong;
	}
	
	
	public static double getHardOutputError(double actual_output[], double desired_output[])
	{
		return (double)(getNumErrors(actual_output, desired_output)) / actual_output.length;
	}
	
	public void addNoisySamples(int num_samples)
	{
		double new_input[][] = new double[num_samples + input.length][input[0].length];
		double new_output[][] = new double[num_samples + output.length][output[0].length];
		for(int i = 0; i < input.length; i++)
		{
			for(int j = 0; j < input[0].length; j++)
			{
				// copy old input into the new training set
				new_input[i][j] = input[i][j];
			}
			for(int j = 0; j < output[0].length; j++)
			{
				// copy old output into the new training set
				new_output[i][j] = output[i][j];
			}
		}
		for(int i = input.length; i < new_input.length; i++)
		{
			int source_index = i % (input.length - 1);
			for(int j = 0; j < new_input[0].length; j++)
			{
				// make a noisy copy of an old input value
				if(input[source_index][j] < 0.5)
				{
					new_input[i][j] = 0.18 + (rand.nextDouble() - 0.5) * 0.18;
				} else {
					new_input[i][j] = 0.92 + (rand.nextDouble() - 0.5) * 0.18;
				}
			}
			 source_index = i % (output.length - 1);
			for(int j = 0; j < new_output[0].length; j++)
			{
				// make a noisy copy of an old output value
				if(input[source_index][j] < 0.5)
				{
					new_output[i][j] =  0.2 + (rand.nextDouble() - 0.5) * 0.25;
				} else {
					new_output[i][j] = 0.8 + (rand.nextDouble() - 0.5) * 0.25;
				}
			}
		}
		input = new_input;
		output = new_output;
	}
}
