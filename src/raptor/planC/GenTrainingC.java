/**
 * 
 */
package raptor.planC;

import raptor.GenTrainingBase;

/**
 * @author sam
 *
 */
public class GenTrainingC extends GenTrainingBase
{

	public static void main(String[] args) throws InterruptedException
	{
		GenTrainingC generator = new GenTrainingC();
		generator.cov3d_on = false;
		Thread.sleep(100);
		generator.run(args);
	}

	@Override
	public void postProcessing()
	{
		genImageCovarianceMatrix(0.01);
	}

}
