package raptor;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.jblas.DoubleMatrix;
import org.jblas.util.Random;

import static raptor.engine.Image.*;
import static raptor.engine.Math.*;

public class experiment3D {
	public static void main(String[] args) throws IOException {
		String int_string = Random.nextInt(100000) + "";
		while(int_string.length() < 6) {
			int_string = "0" + int_string;
		}
		String path = "data/hamina_" + int_string + ".png";
		System.out.println("Loading " + path + "...");
		BufferedImage orig_img = ImageIO.read(new File(path));
		System.out.println("WIDTH: " + orig_img.getWidth());
		System.out.println("HEIGHT: " + orig_img.getHeight());
		PixelGrid grid = normalizeImage(orig_img);
		DoubleMatrix matrix = imageAs3DPointCloud(grid);
		DoubleMatrix cov = getCovarianceMatrix(matrix);
		printMatrix(cov);
	}
}
