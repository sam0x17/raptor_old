package raptor;

import org.jblas.DoubleMatrix;

public class Instance2 {
	public double rx, ry, rz;
	public double true_dist;
	public double r1, r2, r3, r4, r5, r6;
	private String id = null;
	public DoubleMatrix cov2d = null;
	public double[] ref = new double[27];
	
	public void setID(String id) {
		if(id.length() == 8)
			this.id = id;
		else
			throw new RuntimeException("id length must be 8!");
	}
	
	public String id() {
		return this.id;
	}
}
