package raptor;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;

import javax.imageio.ImageIO;

import org.jblas.*;

import static raptor.engine.Image.normalizeImage;
import static raptor.engine.Math.*;

public class LinearMatchExperiment
{
	public boolean cov2d_on = true;
	public static Random rand = new Random(System.currentTimeMillis());
	
	public String data_dir = null;
	
	public static void main(String[] args) throws IOException
	{
		LinearMatchExperiment comp = new LinearMatchExperiment();
		comp.run(args);
	}


	public final void run(String args[]) throws IOException
	{
		data_dir = args[0];
		File folder = new File(data_dir);
		if(!folder.isDirectory())
			throw new IllegalArgumentException("path must be a directory!");

		System.out.println("Loading data from: '" + data_dir + "'...");
		File[] files = folder.listFiles(new FilenameFilter()
		{
			public boolean accept(File dir, String name)
			{
				String lowercaseName = name.toLowerCase();
				if(lowercaseName.endsWith(".txt")) return true;
				else return false;
			}
		});
		System.out.println("Sorting...");
		Arrays.sort(files,new Comparator<File>() { public int compare(File a, File b) { return a.getName().compareTo(b.getName()); } });
		
		// select one instance to linearly match
		int selected_index = rand.nextInt(files.length);
		File selected_file = files[selected_index];
		
		System.out.println("File #" + selected_index + " selected (" + selected_file.getName() + ")");
		Instance selected_instance = loadInstance(selected_file);
		
		System.out.println("Processing " + files.length + " instances...");
		ArrayList<DistanceMatch> distance_matches = new ArrayList<DistanceMatch>();
		for(int i = 0; i < files.length; i++)
		{
			File annotation_file = files[i];
			System.out.println("Processing " + annotation_file.getName() + "...");
			Instance instance = loadInstance(annotation_file);
			DistanceMatch match = new DistanceMatch();
			match.annotation_file = annotation_file;
			match.instance = instance;
			match.dist = matrix_difference_metric(instance.cov2d, selected_instance.cov2d);
			distance_matches.add(match);
		}
		System.out.println("Sorting distances...");
		Collections.sort(distance_matches);
		Collections.reverse(distance_matches);
		for(DistanceMatch match : distance_matches)
		{
			System.out.println(match.annotation_file + "\t" + match.dist + "\t" + match.instance.rx + "\t" + match.instance.ry + "\t" + match.instance.rz + "\t" + match.instance.true_dist);
		}
		System.out.println();
	}
	
	public static class DistanceMatch implements Comparable<DistanceMatch>
	{
		public double dist = 0.0;
		public Instance instance;
		public File annotation_file;
		public int compareTo(DistanceMatch o)
		{
			return new Double(dist).compareTo(o.dist);
		}
	}
	
	public Instance loadInstance(File annotation_file) throws IOException
	{
		Scanner sc = new Scanner(annotation_file);
		String line;
		String[] tokens;
		
		Instance instance = new Instance();
		
		line = sc.nextLine();
		System.out.println(line);
		instance.true_dist = Double.parseDouble(line);
		line = sc.nextLine();
		System.out.println(line);
		tokens = line.split("\t");
		instance.rx = Double.parseDouble(tokens[0]);
		instance.ry = Double.parseDouble(tokens[1]);
		instance.rz = Double.parseDouble(tokens[2]);
		double cov2d_tmp[][] = new double[2][2];
		for(int r = 0; r < cov2d_tmp.length; r++)
		{
			line = sc.nextLine();
			System.out.println(line);
			tokens = line.split("\t");
			for(int c = 0; c < tokens.length; c++)
				cov2d_tmp[r][c] = Double.parseDouble(tokens[c]);
		}
		instance.cov2d = new DoubleMatrix(cov2d_tmp);
		
		sc.close();
		return instance;
	}
}
