package raptor;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Scanner;

import javax.imageio.ImageIO;

import org.jblas.*;

import static raptor.engine.Image.normalizeImage;
import static raptor.engine.Math.*;

//TODO: save data

public class CompileTrainingData
{
	public Instance current_instance = null;
	
	public int main_img_width, main_img_height,
	   alt_img_width, alt_img_height;
	
	public boolean cov2d_on = true;
	public boolean cov3d_on = true;
	public boolean autocrop_on = true;
	public boolean autoresize_on = true;
	public boolean alt_img_on = true;
	
	public HashMap<String, BasicObserverStat<CompileTrainingData>> stats = new HashMap<String, BasicObserverStat<CompileTrainingData>>();

	public String data_dir = null;
	
	public static void main(String[] args) throws IOException
	{
		CompileTrainingData comp = new CompileTrainingData();
		comp.run(args);
	}


	public final void run(String args[]) throws IOException
	{
		registerStats();
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
		System.out.println("Processing " + files.length + " instances...");
		for(File annotation_file : files)
		{
			loadInstance(annotation_file);
			updateStats();
		}
		printStats();
		
	}
	
	public void printStats()
	{
		System.out.println();
		System.out.println("Stats: ");
		System.out.println();
		stats.get("true_dist").prettyPrint("true_dist");
		stats.get("rx").prettyPrint("rx");
		stats.get("ry").prettyPrint("ry");
		stats.get("rz").prettyPrint("rz");
		if(autocrop_on)
		{
			stats.get("crop_f0").prettyPrint("crop_f0");
			stats.get("crop_f1").prettyPrint("crop_f1");
			stats.get("crop_f2").prettyPrint("crop_f2");
			stats.get("crop_f3").prettyPrint("crop_f3");
		}
		if(autoresize_on)
		{
			stats.get("auto_resize_fx").prettyPrint("auto_resize_fx");
			stats.get("auto_resize_fy").prettyPrint("auto_resize_fy");
		}
		if(cov2d_on)
			stats.get("cov2d_entry").prettyPrint("cov2d_entry");
		if(cov3d_on)
			stats.get("cov3d_entry").prettyPrint("cov3d_entry");
		
		/*
		if(alt_img_on)
		{
			stats.get("alt_img_width").prettyPrint("alt_img_width");
			stats.get("alt_img_height").prettyPrint("alt_img_height");
		}*/
	}
	
	public void registerStats()
	{
		stats.clear();
		stats.put("rx", new BasicObserverStat<CompileTrainingData>(){
			public void update(CompileTrainingData parent) {
				addSample(parent.current_instance.rx);
			}
		});
		stats.put("ry", new BasicObserverStat<CompileTrainingData>(){
			public void update(CompileTrainingData parent) {
				addSample(parent.current_instance.ry);
			}
		});
		stats.put("rz", new BasicObserverStat<CompileTrainingData>(){
			public void update(CompileTrainingData parent) {
				addSample(parent.current_instance.rz);
			}
		});
		stats.put("true_dist", new BasicObserverStat<CompileTrainingData>(){
			public void update(CompileTrainingData parent) {
				addSample(parent.current_instance.true_dist);
			}
		});
		stats.put("main_img_width", new BasicObserverStat<CompileTrainingData>(){
			public void update(CompileTrainingData parent) {
				addSample(parent.main_img_width);
			}
		});
		stats.put("main_img_height", new BasicObserverStat<CompileTrainingData>(){
			public void update(CompileTrainingData parent) {
				addSample(parent.main_img_height);
			}
		});
		if(alt_img_on)
		{
			stats.put("alt_img_width", new BasicObserverStat<CompileTrainingData>(){
				public void update(CompileTrainingData parent) {
					addSample(parent.alt_img_width);
				}
			});
			stats.put("alt_img_height", new BasicObserverStat<CompileTrainingData>(){
				public void update(CompileTrainingData parent) {
					addSample(parent.alt_img_height);
				}
			});
		}
		if(autocrop_on)
		{
			stats.put("crop_f0", new BasicObserverStat<CompileTrainingData>(){
				public void update(CompileTrainingData parent) {
					addSample(parent.current_instance.crop_f0);
				}
			});
			stats.put("crop_f1", new BasicObserverStat<CompileTrainingData>(){
				public void update(CompileTrainingData parent) {
					addSample(parent.current_instance.crop_f1);
				}
			});
			stats.put("crop_f2", new BasicObserverStat<CompileTrainingData>(){
				public void update(CompileTrainingData parent) {
					addSample(parent.current_instance.crop_f2);
				}
			});
			stats.put("crop_f3", new BasicObserverStat<CompileTrainingData>(){
				public void update(CompileTrainingData parent) {
					addSample(parent.current_instance.crop_f3);
				}
			});	
		}
		if(autoresize_on)
		{
			stats.put("auto_resize_fx", new BasicObserverStat<CompileTrainingData>(){
				public void update(CompileTrainingData parent) {
					addSample(parent.current_instance.auto_resize_fx);
				}
			});
			stats.put("auto_resize_fy", new BasicObserverStat<CompileTrainingData>(){
				public void update(CompileTrainingData parent) {
					addSample(parent.current_instance.auto_resize_fy);
				}
			});
		}
		if(cov2d_on)
		{
			stats.put("cov2d_entry", new BasicObserverStat<CompileTrainingData>(){
				public void update(CompileTrainingData parent) {
					for(double entry : parent.current_instance.cov2d.data)
						addSample(entry);
				}
			});
		}
		if(cov3d_on)
		{
			stats.put("cov3d_entry", new BasicObserverStat<CompileTrainingData>(){
				public void update(CompileTrainingData parent) {
					for(double entry : parent.current_instance.cov3d.data)
						addSample(entry);
				}
			});
		}
	}
	
	public void loadInstance(File annotation_file) throws IOException
	{
		Scanner sc = new Scanner(annotation_file);
		String line;
		String[] tokens;
		
		current_instance = new Instance();
		
		current_instance.true_dist = Double.parseDouble(sc.nextLine());
		line = sc.nextLine();
		tokens = line.split("\t");
		current_instance.rx = Double.parseDouble(tokens[0]);
		current_instance.ry = Double.parseDouble(tokens[1]);
		current_instance.rz = Double.parseDouble(tokens[2]);
		if(autocrop_on)
		{
			line = sc.nextLine();
			tokens = line.split("\t");
			current_instance.crop_f0 = Double.parseDouble(tokens[0]);
			current_instance.crop_f1 = Double.parseDouble(tokens[1]);
			current_instance.crop_f2 = Double.parseDouble(tokens[2]);
			current_instance.crop_f3 = Double.parseDouble(tokens[3]);
		}
		if(autoresize_on)
		{
			line = sc.nextLine();
			tokens = line.split("\t");
			current_instance.auto_resize_fx = Double.parseDouble(tokens[0]);
			current_instance.auto_resize_fy = Double.parseDouble(tokens[1]);
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
			current_instance.cov3d = new DoubleMatrix(cov3d_tmp);
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
			current_instance.cov2d = new DoubleMatrix(cov2d_tmp);
		}
		
		/*String main_img_path = annotation_file.getAbsolutePath().substring(0, annotation_file.getAbsolutePath().length() - 3) + "png";
		File main_img_file = new File(main_img_path);
		System.out.println("Loading " + main_img_file.getName() + "...");
		BufferedImage main_img = ImageIO.read(main_img_file);
		PixelGrid orig_img_grid = normalizeImage(main_img);*/
		
		if(alt_img_on)
		{
			String alt_img_path = annotation_file.getAbsolutePath().substring(0, annotation_file.getAbsolutePath().length() - 4) + "_crop.png";
			File alt_img_file = new File(alt_img_path);
			System.out.println("loading " + alt_img_file.getName());
			BufferedImage alt_img = ImageIO.read(alt_img_file);
			alt_img_height = alt_img.getHeight();
			alt_img_width = alt_img.getWidth();
			current_instance.alt_img = normalizeImage(alt_img);
		}
		sc.close();
	}
	
	public final void updateStats()
	{
		for(BasicObserverStat<CompileTrainingData> stat : stats.values())
			stat.update(this);
	}
}
