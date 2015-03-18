/**
 * 
 */
package raptor.planC;

import java.io.FileNotFoundException;

import raptor.GenTrainingBase;

/**
 * @author sam
 *
 */
public class GenTrainingC extends GenTrainingBase
{

	public static void main(String[] args) throws InterruptedException, FileNotFoundException
	{
		GenTrainingC generator = new GenTrainingC();
		generator.cov3d_on = false;
		generator.run(args);
	}

	@Override
	public void postProcessing()
	{
		genImageCovarianceMatrix(0.01);
	}

}
