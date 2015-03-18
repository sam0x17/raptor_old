package raptor.planB;

import java.io.FileNotFoundException;

import raptor.GenTrainingBase;

public class GenTrainingB extends GenTrainingBase
{
	
	public static void main(String[] args) throws FileNotFoundException
	{
		GenTrainingB generator = new GenTrainingB();
		try {
			generator.run(args);
		} catch (Throwable e) {
			generator.logError(e.getMessage());
		}
		
	}

	@Override
	public void postProcessing()
	{
		//genImageCovarianceMatrix(0.01);
		//autoCrop();
		//autoResize();
		//writeAlternateImage("_crop");
		genImageCovarianceMatrix(0.01);
		genImageSpecialCovarianceMatrix();

	}

}
