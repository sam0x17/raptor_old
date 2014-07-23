package raptor.planB;

import raptor.GenTrainingBase;

public class GenTrainingB extends GenTrainingBase
{
	
	public static void main(String[] args)
	{
		GenTrainingB generator = new GenTrainingB();
		generator.run(args);
	}

	@Override
	public void postProcessing()
	{
		genImageCovarianceMatrix(0.01);
		autoCrop();
		autoResize();
		writeAlternateImage("_crop");
	}

}
