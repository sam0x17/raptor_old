package raptor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Scanner;

import org.jblas.*;

import static raptor.engine.Math.*;

public class CompileTrainingData
{
	public double rx, ry, rz,
				  true_dist,
				  crop_f0, crop_f1, crop_f2, crop_f3,
				  auto_resize_fx, auto_resize_fy;
	
	public DoubleMatrix cov2d = null;
	public DoubleMatrix cov3d = null;
	
	public int main_img_width, main_img_height,
			   alt_img_width, alt_img_height;
	
	public boolean cov2d_on = true;
	public boolean cov3d_on = true;
	public boolean autocrop_on = true;
	public boolean autoresize_on = true;
	public boolean alt_img_on = true;
	
	public HashMap<String, BasicObserverStat<CompileTrainingData>> stats;
	

	/*
	public BasicStat stat_rx, stat_ry, stat_rz,
					 stat_true_dist,
					 stat_mainimg_width, stat_mainimg_height,
					 stat_altimg_width, stat_altimg_height,
					 stat_crop0, stat_crop1, stat_crop2, stat_crop3,
					 stat_rex, stat_rey,
					 stat_cov3d_entry, stat_cov2d_entry;*/

	public String data_dir = null;
	
	public static void main(String[] args) throws FileNotFoundException
	{
		CompileTrainingData comp = new CompileTrainingData();
		comp.run(args);
	}


	public final void run(String args[]) throws FileNotFoundException
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

	}
	
	public void registerStats()
	{
		stats.clear();
		stats.put("rx", new BasicObserverStat<CompileTrainingData>(){
			public void update(CompileTrainingData parent) {
				addSample(parent.rx);
			}
		});
		stats.put("ry", new BasicObserverStat<CompileTrainingData>(){
			public void update(CompileTrainingData parent) {
				addSample(parent.ry);
			}
		});
		stats.put("rz", new BasicObserverStat<CompileTrainingData>(){
			public void update(CompileTrainingData parent) {
				addSample(parent.rz);
			}
		});
		stats.put("true_dist", new BasicObserverStat<CompileTrainingData>(){
			public void update(CompileTrainingData parent) {
				addSample(parent.true_dist);
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
					addSample(parent.crop_f0);
				}
			});
			stats.put("crop_f1", new BasicObserverStat<CompileTrainingData>(){
				public void update(CompileTrainingData parent) {
					addSample(parent.crop_f1);
				}
			});
			stats.put("crop_f2", new BasicObserverStat<CompileTrainingData>(){
				public void update(CompileTrainingData parent) {
					addSample(parent.crop_f2);
				}
			});
			stats.put("crop_f3", new BasicObserverStat<CompileTrainingData>(){
				public void update(CompileTrainingData parent) {
					addSample(parent.crop_f3);
				}
			});	
		}
		if(autoresize_on)
		{
			stats.put("auto_resize_fx", new BasicObserverStat<CompileTrainingData>(){
				public void update(CompileTrainingData parent) {
					addSample(parent.auto_resize_fx);
				}
			});
			stats.put("auto_resize_fy", new BasicObserverStat<CompileTrainingData>(){
				public void update(CompileTrainingData parent) {
					addSample(parent.auto_resize_fy);
				}
			});
		}
		if(cov2d_on)
		{
			stats.put("cov2d_entry", new BasicObserverStat<CompileTrainingData>(){
				public void update(CompileTrainingData parent) {
					for(double entry : parent.cov2d.data)
						addSample(entry);
				}
			});
		}
		if(cov3d_on)
		{
			stats.put("cov3d_entry", new BasicObserverStat<CompileTrainingData>(){
				public void update(CompileTrainingData parent) {
					for(double entry : parent.cov3d.data)
						addSample(entry);
				}
			});
		}
	}
	
	public void loadInstance(File annotation_file) throws FileNotFoundException
	{
		Scanner sc = new Scanner(annotation_file);
		while(sc.hasNext())
		{
			String line = sc.nextLine();
			String[] tokens = line.split("\t");
			
		}
	}
	
	public final void updateStats()
	{
		for(BasicObserverStat<CompileTrainingData> stat : stats.values())
			stat.update(this);
	}
}
