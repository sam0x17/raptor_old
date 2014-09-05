package raptor;

import org.jblas.DoubleMatrix;

import raptor.engine.Image.PixelGrid;

public class Instance
{
	public double rx, ry, rz,
				  true_dist,
				  crop_f0, crop_f1, crop_f2, crop_f3,
				  auto_resize_fx, auto_resize_fy;

	public DoubleMatrix cov2d = null;
	public DoubleMatrix cov3d = null;
	
	PixelGrid main_img = null;
	PixelGrid alt_img = null;
}
