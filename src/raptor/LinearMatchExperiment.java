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
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;

import javax.imageio.ImageIO;

import org.jblas.*;

import static raptor.engine.Image.normalizeImage;
import static raptor.engine.Math.*;

public class LinearMatchExperiment
{
	public boolean cov2d_on = true;
	public boolean cov3d_on = false;
	public boolean cov2d_special_on = true;
	public boolean autocrop_on = false;
	public boolean autoresize_on = false;
	public boolean alt_img_on = false;
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

		System.out.println("Getting filenames from: '" + data_dir + "'...");
		File[] files = folder.listFiles(new FilenameFilter()
		{
			public boolean accept(File dir, String name)
			{
				String lowercaseName = name.toLowerCase();
				if(lowercaseName.endsWith(".txt")) return true;
				else return false;
			}
		});
		System.out.println("Found " + files.length + " samples");
		System.out.println("Sorting filenames...");
		Arrays.sort(files,new Comparator<File>() { public int compare(File a, File b) { return a.getName().compareTo(b.getName()); } });
		
		System.out.println("Loading data...");
		ArrayList<DistanceMatch> instances = new ArrayList<DistanceMatch>();
		for(int i = 0; i < files.length; i++)
		{
			File annotation_file = files[i];
			Instance instance;
			try {
				instance = loadInstance(annotation_file);
			} catch (Exception e) {
				System.out.println("Corrupted -- skipping " + annotation_file.getName());
				continue;
			}
			DistanceMatch match = new DistanceMatch();
			match.annotation_file = annotation_file;
			match.instance = instance;
			match.dist = 0.0;
			instances.add(match);
		}

		System.out.println("Found " + instances.size() + " uncorrupted samples");
		
		HashSet<Integer> already_chosen = new HashSet<Integer>();
		int num_iterations = 100;
		int top_hits_to_consider = 500;
		
		double avg_dist_offset = 0.0;
		double avg_rx_offset = 0.0;
		double avg_ry_offset = 0.0;
		double avg_rz_offset = 0.0;
		double avg_dist_pct_error = 0.0;
		double avg_rx_pct_error = 0.0;
		double avg_ry_pct_error = 0.0;
		double avg_rz_pct_error = 0.0;
		
		System.out.println("Running iterations...");
		
		int times_best_match_found = 0;
		
		for(int iteration = 0; iteration < num_iterations; iteration++)
		{
			// select one instance to linearly match
			int selected_index = rand.nextInt(files.length);
			while(already_chosen.contains(selected_index))
			{
				selected_index = rand.nextInt(files.length);
			}
			File selected_file = files[selected_index];
			System.out.println("File #" + selected_index + " selected\t(" + selected_file.getName() + ")");
			Instance selected_instance = loadInstance(selected_file);
			
			ArrayList<DistanceMatch> distance_matches = new ArrayList<DistanceMatch>();
			for(int i = 0; i < instances.size(); i++)
			{
				DistanceMatch ref = instances.get(i);
				DistanceMatch match = new DistanceMatch();
				match.annotation_file = ref.annotation_file;
				match.instance = ref.instance;
				match.dist = matrix_difference_metric(match.instance.cov2d, selected_instance.cov2d);
				distance_matches.add(match);
			}
			
			Collections.sort(distance_matches);
			Collections.reverse(distance_matches);
			
			double offset_best = Double.MAX_VALUE;
			Instance best_match = null;
			int best_match_index = -1;
			
			for(int i = 0; i < distance_matches.size() - 1; i++)
			{
				Instance hit = distance_matches.get(i).instance;
				double offset = Math.abs(selected_instance.true_dist - hit.true_dist) / selected_instance.true_dist +
						        Math.abs(selected_instance.rx - hit.rx) / selected_instance.rx +
						        Math.abs(selected_instance.ry - hit.ry) / selected_instance.ry +
						        Math.abs(selected_instance.rz - hit.rz) / selected_instance.rz;
				if(offset < offset_best)
				{
					offset_best = offset;
					best_match = hit;
					best_match_index = i;
				}
			}
			
			double dist_offset_best = Math.abs(selected_instance.true_dist - best_match.true_dist);
			double rx_offset_best = Math.abs(selected_instance.rx - best_match.rx);
			double ry_offset_best = Math.abs(selected_instance.ry - best_match.ry);
			double rz_offset_best = Math.abs(selected_instance.rz - best_match.rz);

			double dist_pct_error = dist_offset_best / best_match.true_dist;
			double rx_pct_error = rx_offset_best / best_match.rx;
			double ry_pct_error = ry_offset_best / best_match.ry;
			double rz_pct_error = rz_offset_best / best_match.rz;
			
			avg_dist_offset += dist_offset_best;
			avg_rx_offset += rx_offset_best;
			avg_ry_offset += ry_offset_best;
			avg_rz_offset += rz_offset_best;

			avg_dist_pct_error += dist_pct_error;
			avg_rx_pct_error += rx_pct_error;
			avg_ry_pct_error += ry_pct_error;
			avg_rz_pct_error += rz_pct_error;
			
			System.out.println(best_match_index);
			for(int i = 0; i < top_hits_to_consider; i++)
			{
				if(distance_matches.size() - 1 - i == best_match_index)
				{
					System.out.println("yeah");
					times_best_match_found++;
					break;
				}
			}
		}
		
		avg_dist_offset /= (double)num_iterations;
		avg_rx_offset /= (double)num_iterations;
		avg_ry_offset /= (double)num_iterations;
		avg_rz_offset /= (double)num_iterations;

		avg_dist_pct_error /= (double)num_iterations;
		avg_rx_pct_error /= (double)num_iterations;
		avg_ry_pct_error /= (double)num_iterations;
		avg_rz_pct_error /= (double)num_iterations;
		
		double percent_best_match_found = (double)(times_best_match_found) / (double)(num_iterations) * 100.0;
		
		System.out.println();
		
		System.out.println("   best match avg_rx_offset: " + avg_rx_offset + " (" + (avg_rx_pct_error * 100.0) + "%)");
		System.out.println("   best match avg_ry_offset: " + avg_ry_offset + " (" + (avg_ry_pct_error * 100.0) + "%)");
		System.out.println("   best match avg_rz_offset: " + avg_rz_offset + " (" + (avg_rz_pct_error * 100.0) + "%)");
		System.out.println(" best match avg_dist_offset: " + avg_dist_offset + " (" + (avg_dist_pct_error * 100.0) + "%)");
		
		System.out.println();
		
		System.out.println("best match found: " + percent_best_match_found + "%");
		
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
		
		line = sc.nextLine();;
		instance.true_dist = Double.parseDouble(line);
		line = sc.nextLine();
		tokens = line.split("\t");
		instance.rx = Double.parseDouble(tokens[0]);
		instance.ry = Double.parseDouble(tokens[1]);
		instance.rz = Double.parseDouble(tokens[2]);
		if(autocrop_on)
		{
			line = sc.nextLine();
			tokens = line.split("\t");
			instance.crop_f0 = Double.parseDouble(tokens[0]);
			instance.crop_f1 = Double.parseDouble(tokens[1]);
			instance.crop_f2 = Double.parseDouble(tokens[2]);
			instance.crop_f3 = Double.parseDouble(tokens[3]);
		}
		if(autoresize_on)
		{
			line = sc.nextLine();
			tokens = line.split("\t");
			instance.auto_resize_fx = Double.parseDouble(tokens[0]);
			instance.auto_resize_fy = Double.parseDouble(tokens[1]);
		}
                if(cov2d_on)
                {
                        double cov2d_tmp[][] = new double[2][2];
                        for(int r = 0; r < cov2d_tmp.length; r++)
                        {
                                line = sc.nextLine();
                                tokens = line.split("\t");
                                for(int c = 0; c < tokens.length; c++)
                                        cov2d_tmp[r][c] = Double.parseDouble(tokens[c]);
                        }
                        instance.cov2d = new DoubleMatrix(cov2d_tmp);
                }
		if(cov3d_on)
		{
			double cov3d_tmp[][] = new double[3][3];
			for(int r = 0; r < cov3d_tmp.length; r++)
			{
				line = sc.nextLine();
				tokens = line.split("\t");
				for(int c = 0; c < tokens.length; c++)
					cov3d_tmp[r][c] = Double.parseDouble(tokens[c]);
			}
			instance.cov3d = new DoubleMatrix(cov3d_tmp);
		}
		if(cov2d_special_on)
		{
			double cov2ds_tmp[][] = new double[3][3];
			for(int r = 0; r < cov2ds_tmp.length; r++)
			{
				line = sc.nextLine();
				tokens = line.split("\t");
				for(int c = 0; c < tokens.length; c++)
					cov2ds_tmp[r][c] = Double.parseDouble(tokens[c]);
			}
			instance.cov2d_special = new DoubleMatrix(cov2ds_tmp);
		}
		sc.close();
		return instance;
	}
}
