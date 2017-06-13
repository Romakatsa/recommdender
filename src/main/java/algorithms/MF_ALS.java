package algorithms;

import data_structure.Rating;
import data_structure.SparseMatrix;
import data_structure.DenseVector;
import data_structure.DenseMatrix;
import data_structure.Pair;
import data_structure.SparseVector;
import happy.coding.math.Randoms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.stream.IntStream;

import utils.Printer;


public class MF_ALS {



    int factors = 10; 	// number of latent factors.
	int maxIter = 100; 	// maximum iterations.
	double w0 = 0.01;	// weight for 0s
	double reg = 0.01; 	// regularization parameters
    int userCount;
    int itemCount;

    SparseMatrix trainMatrix;

/** Model parameters to learn */

    DenseMatrix U;	// latent vectors for user
    DenseMatrix V;	// latent vectors for items
  

/** Caches */

	DenseMatrix SU;
	DenseMatrix SV;
	
	boolean showProgress;
	boolean showLoss;
	
	public MF_ALS(SparseMatrix trainMatrix, int factors, int maxIter, double w0, double reg,
                  boolean showProgress, boolean showLoss) {

        this.userCount = trainMatrix.length()[0];
        this.itemCount = trainMatrix.length()[1];
		this.factors = factors;
		this.maxIter = maxIter;
		this.w0 = w0 / itemCount;
		this.reg = reg;
        this.trainMatrix = trainMatrix;
		this.showProgress = showProgress;
		this.showLoss = showLoss;
		this.initialize();
	}
	
	//remove
	public void setUV(DenseMatrix U, DenseMatrix V) {
		this.U = U.clone();
		this.V = V.clone();
		SU = U.transpose().mult(U);
		SV = V.transpose().mult(V);
	}
	
	private void initialize() {
		U = new DenseMatrix(userCount, factors);
		V = new DenseMatrix(itemCount, factors);
		U.init(0, 0.1);
		V.init(0, 0.1);
		
		SU = U.transpose().mult(U);
		SV = V.transpose().mult(V);
	}
	
	// Implement the ALS algorithm of the ICDM'09 paper
	public void buildModel() {
		System.out.println("Run for MF_ALS");
		
		double loss_pre = Double.MAX_VALUE;
		for (int iter = 0; iter < maxIter; iter ++) {
			Long start = System.currentTimeMillis();
			
			// Update user factors

            IntStream.range(0,userCount).parallel().peek(i->update_user(i)).forEach(j->{});

            /*
			for (int u = 0; u < userCount; u ++) {
				update_user(u);
			}
			*/

            if (showProgress) {
                System.out.println("Users updated for " + (System.currentTimeMillis() - start) + "millis");
                start = System.currentTimeMillis();
            }

            IntStream.range(0,itemCount).parallel().peek(i->update_item(i)).forEach(j->{});
			// Update item factors
			/*
            for (int i = 0; i < itemCount; i ++) {
				update_item(i);
			}
			*/

            if (showProgress) {
                System.out.println("Items updated for " + (System.currentTimeMillis() - start) + "millis");
                start = System.currentTimeMillis();
            }

			// Show progress
			if (showProgress)
                System.out.println();
            // Show loss
			if (showLoss)
				loss_pre = showLoss(iter, start, loss_pre);
			
		}
	}
	
	// Run model for one iteration
	public void runOneIteration() {
		// Update user latent vectors
		for (int u = 0; u < userCount; u ++) {
			update_user(u);
		}
		
		// Update item latent vectors
		for (int i = 0; i < itemCount; i ++) {
			update_item(i);
		}
	}
	
	private void update_user(int u) {
		ArrayList<Integer> itemList = trainMatrix.getRowRef(u).indexList();
		// Get matrix Au
		DenseMatrix Au = SU.scale(w0);
		for (int k1 = 0; k1 < factors; k1 ++) {
			for (int k2 = 0; k2 < factors; k2 ++) {
				for (int i : itemList)
					Au.add(k1, k2, V.get(i, k1) * V.get(i, k2) * (1 - w0));
			}
		} 
		// Get vector du
		DenseVector du = new DenseVector(factors);
		for (int k = 0; k < factors; k ++) {
			for (int i : itemList)
				du.add(k, V.get(i, k) * trainMatrix.getValue(u, i));
		}
		// Matrix inversion to get the new embedding
		for (int k = 0; k < factors; k ++) { // consider the regularizer
			Au.add(k, k, reg);
		}
		DenseVector newVector = Au.inv().mult(du);
		
		// Update the SU cache
		for (int f = 0; f < factors; f ++) {
			for (int k = 0; k <= f; k ++) {
				double val = SU.get(f, k) - U.get(u, f) * U.get(u, k)
						+ newVector.get(f) * newVector.get(k);
				SU.set(f, k, val);
				SU.set(k, f, val);
			}
		}
		// Update parameters
		for (int k = 0; k < factors; k ++) {
			U.set(u, k, newVector.get(k));
		}
	}
	
	private void update_item(int i) {
		ArrayList<Integer> userList = trainMatrix.getColRef(i).indexList();
		// Get matrix Ai
		DenseMatrix Ai = SV.scale(w0);
		for (int k1 = 0; k1 < factors; k1 ++) {
			for (int k2 = 0; k2 < factors; k2 ++) {
				for (int u : userList)
					Ai.add(k1, k2, U.get(u, k1) * U.get(u, k2) * (1 - w0));
			}
		}
		// Get vector di
		DenseVector di = new DenseVector(factors);
		for (int k = 0; k < factors; k ++) {
			for (int u : userList)
				di.add(k, U.get(u, k) * trainMatrix.getValue(u, i));
		}
		// Matrix inversion to get the new embedding
		for (int k = 0; k < factors; k ++) { // consider the regularizer
			Ai.add(k, k, reg);
		}
		DenseVector newVector = Ai.inv().mult(di);
		
		// Update the SV cache
		for (int f = 0; f < factors; f ++) {
			for (int k = 0; k <= f; k ++) {
				double val = SV.get(f, k) - V.get(i, f) * V.get(i, k)
						+ newVector.get(f) * newVector.get(k);
				SV.set(f, k, val);
				SV.set(k, f, val);
			}
		}
		
		// Update parameters
		for (int k = 0; k < factors; k ++) {
			V.set(i, k, newVector.get(k));
		}
	}
	
	public double showLoss(int iter, long start, double loss_pre) {
		long start1 = System.currentTimeMillis();
		double loss_cur = loss();
		String symbol = loss_pre >= loss_cur ? "-" : "+";
		System.out.printf("Iter=%d [%s]\t [%s]loss: %.4f [%s]\n", iter, 
				Printer.printTime(start1 - start), symbol, loss_cur, 
				Printer.printTime(System.currentTimeMillis() - start1));
		return loss_cur;
	}
	
	// Fast way to calculate the loss function
	public double loss() {
		// Init the SV cache for fast calculation
		DenseMatrix SV = new DenseMatrix(factors, factors);
		for (int f = 0; f < factors; f ++) {
			for (int k = 0; k <= f; k ++) {
				double val = 0;
				for (int i = 0; i < itemCount; i ++)
					val += V.get(i, f) * V.get(i, k);
				SV.set(f, k, val);
				SV.set(k, f, val);
			}
		}
		
		double L = reg * (U.squaredSum() + V.squaredSum());
		for (int u = 0; u < userCount; u ++) {
			double l = 0;
			for (int i : trainMatrix.getRowRef(u).indexList()) {
				l += Math.pow(trainMatrix.getValue(u, i) - predict(u, i), 2);
			}
			l *= (1 - w0);
			l += w0 * SV.mult(U.row(u, false)).inner(U.row(u, false));
			L += l;
		}
		
		return L;
	}
	

	public double predict(int u, int i) {
		return U.row(u, false).inner(V.row(i, false));
	}

    public DenseMatrix getUserFactors() {
        return this.U;
    }

    public DenseMatrix getItemFactors() { return this.V.transpose(); }


	public ArrayList<Integer> getRecommendedItemsById(int id, int topN, boolean ignore) {
		DenseVector a = V.mult(U.row(id,false));
		ArrayList<Integer> ignored = trainMatrix.getRowRef(id).indexList();
		if (ignore) {
			return a.topIndicesByValue(topN,ignored);
		}
		else {
			return a.topIndicesByValue(topN,null);
		}

	}

}

