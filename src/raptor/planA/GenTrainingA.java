/**
 * 
 */
package raptor.planA;

import java.io.FileNotFoundException;

import raptor.GenTrainingBase;
import raptor.planB.GenTrainingB;

/**
 * @author sam
 *
 */
public class GenTrainingA extends GenTrainingBase
{

	public static void main(String[] args) throws FileNotFoundException
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
