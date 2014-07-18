/**
 * Called by render.py -- processes and saves the data
 * for a generated training instance for later use
 */
package raptor;

import static raptor.engine.Math.*;
import static raptor.engine.Image.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.jblas.*;
 
/**
 * @author Sam Kelly
 */
public class test_script
{
	public static void main(String[] args) throws IOException
	{
		DoubleMatrix data = new DoubleMatrix(new double[][]{{0, 1, 0, 1, 3},
															{1, 2, 3, 0, 2},
															{4, 1 ,2, 1, 1}});
		
		//BufferedImage img = ImageIO.read(new File("/home/sam/Desktop/gp1/RAPTOR/DATA/hamina_000000.png"));
		BufferedImage img = ImageIO.read(new File("/home/skelly/RAPTOR/DATA/hamina_000002.png"));
		System.out.println("width: " + img.getWidth() + "  height: " + img.getHeight() + "\n");
		PixelGrid grid = normalizeImage(img);
		DoubleMatrix img_matrix = imageAs2DPointCloud(grid, 0.01);
		printMatrix(getCovarianceMatrix(img_matrix), "2D covariance matrix");
		
		printMatrix(getCovarianceMatrix(data), "D covariance matrix");
		
	}
}
