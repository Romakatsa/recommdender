package algorithms;

import java.util.ArrayList;
import java.util.HashMap;

import data_structure.Rating;
import data_structure.SparseMatrix;

public class ItemPopularity {
    int itemCount;
    int userCount;
    SparseMatrix trainMatrix;
	double[] itemPopularity;
	public ItemPopularity(SparseMatrix trainMatrix) {

        this.userCount = trainMatrix.length()[0];
        this.itemCount = trainMatrix.length()[1];
		itemPopularity = new double[itemCount];
		this.trainMatrix = trainMatrix;
	}
	
	public void buildModel() {
		for (int i = 0; i < itemCount; i++) {
			// Measure popularity by number of reviews received.
			itemPopularity[i] = trainMatrix.getColRef(i).itemCount();
		}
	}
	
	public double predict(int u, int i) {
		return itemPopularity[i];
	}

    public double[] getItemPopularity() {
	    return itemPopularity;
    }
}

