/**
 * 
 */
package raptor;

/**
 * @author sam
 *
 */
public abstract class TrainBase
{
	public double min_cov3d_val = Double.MAX_VALUE;
	public double max_cov3d_val = Double.MIN_VALUE;
	
	public double min_cov2d_val = Double.MAX_VALUE;
	public double max_cov2d_val = Double.MIN_VALUE;
	
	public double min_true_dist_val = Double.MAX_VALUE;
	public double max_true_dist_val = Double.MIN_VALUE;
	
	public double min_rot_val = Math.PI * 2.0;
	
	
	public final void run(String args[])
	{
		
	}
	
	public void preScan(boolean cov3d_on, boolean cov2d_on, boolean autoresize_on, boolean autocrop_on)
	{
		
	}

}
