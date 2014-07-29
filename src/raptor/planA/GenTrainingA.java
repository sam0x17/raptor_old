/**
 * 
 */
package raptor.planA;

import raptor.GenTrainingBase;
import raptor.planB.GenTrainingB;

/**
 * @author sam
 *
 */
public class GenTrainingA extends GenTrainingBase
{

	public static void main(String[] args)
	{
		GenTrainingB generator = new GenTrainingB();
		generator.run(args);
	}

	@Override
	public void postProcessing()
	{
		genImageCovarianceMatrix(0.005);
		autoCrop();
		autoResize();
		writeAlternateImage("_crop");
	}

}
