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
import java.util.HashMap;
import java.util.stream.IntStream;

import utils.Printer;



public class MF_fastALS {

    SparseMatrix trainMatrix;

    int factors = 10; 	// number of latent factors.
    int maxIter = 500; 	// maximum iterations.
    double reg = 0.01; 	// regularization parameters
    double defaultWeight = 0.1;


    /** Model parameters to learn */
    public DenseMatrix U;	// latent vectors for users
    public DenseMatrix V;	// latent vectors for items


    /** Caches */
    DenseMatrix SU;
    DenseMatrix SV;
    double[] prediction_users;//, prediction_items;
    double[] rating_users;//, rating_items;
    double[] w_users;//, w_items;


    // weight for each positive instance in trainMatrix
    SparseMatrix W;
    double[] W_neg;                 //negative instance weights
    int itemCount;
    int userCount;
    int weightStrategy;
    double alpha;


    boolean showProgress;
    boolean showLoss;




    public MF_fastALS(SparseMatrix trainMatrix, int factors, int maxIter, double w0, double alpha, double reg,
                      boolean showProgress, boolean showLoss) {

        this.factors = factors;
        this.maxIter = maxIter;
        this.defaultWeight = w0;
        this.reg = reg;
        this.showLoss = showLoss;
        this.showProgress = showProgress;
        this.userCount = trainMatrix.length()[0];
        this.itemCount = trainMatrix.length()[1];
        this.trainMatrix = trainMatrix;
        this.alpha = alpha;

        // Init caches
        //prediction_users = new double[userCount];
        //prediction_items = new double[itemCount];
        //rating_users = new double[userCount];
        //rating_items = new double[itemCount];
        //w_users = new double[userCount];
        //w_items = new double[itemCount];

        // Init model parameters
        U = new DenseMatrix(userCount, factors);
        V = new DenseMatrix(itemCount, factors);
        U.init(0, 0.1);
        V.init(0, 0.1);
        initS();
    }

    public void setWeights(int type) {
        this.weightStrategy = type;
        double[] p;
        double sum, Z;
        double min = Integer.MAX_VALUE;
        double max = Integer.MIN_VALUE;
        int max_i=0;
        int min_i=0;
        // Positive part

        W = new SparseMatrix(userCount, itemCount);
        for (int u = 0; u < userCount; u ++)
            for (int i : trainMatrix.getRowRef(u).indexList())
                W.setValue(u, i, 1);


        // Negative part

        switch(type) {
            case 0:    //Uniformly
                break;

            case 1:     //Item-Oriented

                sum = 0;
                Z = 0;
                p = new double[itemCount];
                for (int i = 0; i < itemCount; i ++) {
                    p[i] = trainMatrix.getColRef(i).itemCount();
                    sum += p[i];
                }
                // convert p[i] to probability
                for (int i = 0; i < itemCount; i ++) {
                    p[i] /= sum;
                    p[i] = Math.pow(p[i], alpha);
                    Z += p[i];
                }

                W_neg = new double[itemCount];
                for (int i = 0; i < itemCount; i ++) {
                    W_neg[i] = defaultWeight * p[i] / Z;
                    if (W_neg[i] > max) {
                        max = W_neg[i];
                        max_i = i;
                    }
                    if (W_neg[i] < min) {
                        min = W_neg[i];
                        min_i=i;
                    }
                }
                System.out.println("Min W:" + min);
                System.out.println("Max W:" + max);

                break;

            case 2:     //User-Oriented
                /*sum = 0;
                p = new double[userCount];
                for (int u = 0; u < userCount; u ++) {
                    p[u] = trainMatrix.getRowRef(u).itemCount();
                    sum += p[u];
                }
                double mean = sum / userCount;
                */

                sum = 0;
                Z = 0;
                p = new double[userCount];
                for (int i = 0; i < userCount; i ++) {
                    p[i] = trainMatrix.getRowRef(i).itemCount();
                    sum += p[i];
                }
                // convert p[i] to probability
                for (int i = 0; i < userCount; i ++) {
                    p[i] /= sum;
                    p[i] = Math.pow(1-p[i], alpha);
                    Z += p[i];
                }

                W_neg = new double[userCount];
                for (int i = 0; i < userCount; i ++) {
                    W_neg[i] = defaultWeight * p[i] / Z;
                    if (W_neg[i] > max) {
                        max = W_neg[i];
                        max_i = i;
                    }
                    if (W_neg[i] < min) {
                        min = W_neg[i];
                        min_i = i;
                    }
                }
                //System.out.println("Min W: " + min+ " on id "+min_i);
                //System.out.println("Max W: " + max+ " on id "+max_i);
                break;


            default:
                break;
        }

    }

    private double getNegWeight(int i) {

        if (weightStrategy == 0) {
            return this.defaultWeight;
        }
        else {
            return W_neg[i];
        }

    }

    /*
    public void setTrain(SparseMatrix trainMatrix) {
        this.trainMatrix = new SparseMatrix(trainMatrix);
        W = new SparseMatrix(userCount, itemCount);
        for (int u = 0; u < userCount; u ++)
            for (int i : this.trainMatrix.getRowRef(u).indexList())
                W.setValue(u, i, 1);
    }
    */

    // Init SU and SV
    private void initS() {
        if (weightStrategy != 2) {
            SU = U.transpose().mult(U);
            // Init SV as V^T W_neg V
            SV = new DenseMatrix(factors, factors);
            for (int f = 0; f < factors; f++) {
                for (int k = 0; k <= f; k++) {
                    double val = 0;
                    for (int i = 0; i < itemCount; i++)
                        val += V.get(i, f) * V.get(i, k) * getNegWeight(i);
                    SV.set(f, k, val);
                    SV.set(k, f, val);
                }
            }
        }
        else {
            SV = V.transpose().mult(V);
            // Init SV as V^T W_neg V
            SU = new DenseMatrix(factors, factors);
            for (int f = 0; f < factors; f++) {
                for (int k = 0; k <= f; k++) {
                    double val = 0;
                    for (int i = 0; i < userCount; i++)
                        val += U.get(i, f) * U.get(i, k) * getNegWeight(i);
                    SU.set(f, k, val);
                    SU.set(k, f, val);
                }
            }
        }
    }


    public void buildModel() {
        //System.out.println("Run for FastALS. ");
        double loss_pre = Double.MAX_VALUE;
        for (int iter = 0; iter < maxIter; iter ++) {
            Long start = System.currentTimeMillis();
            if (showProgress)
                System.out.println("Iteration "+(iter+1)+" started");
            // Update user latent vectors
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
            // Update item latent vectors
            /*
            for (int i = 0; i < itemCount; i ++) {
                update_item(i);
            }
            */
            if (showProgress) {
                System.out.println("Items updated for " + (System.currentTimeMillis() - start) + "millis");
                start = System.currentTimeMillis();
            }

            // Show loss
            if (showLoss)
                loss_pre = showLoss(iter, start, loss_pre);


        } // end for iter

    }


    // Run model for one iteration
    public void runOneIteration() {
        // Update user latent vectors

        //IntStream.range(0,userCount).peek(i->update_user(i)).forEach(j->{});

        for (int u = 0; u < userCount; u ++) {
            update_user(u);
        }

        // Update item latent vectors
        for (int i = 0; i < itemCount; i ++) {
            update_item(i);
        }
    }



    protected void update_user(int u) {
        ArrayList<Integer> itemList = trainMatrix.getRowRef(u).indexList();
        if (itemList.size() == 0) return;    // user has no ratings
        // prediction cache for the user

        double[] prediction_items = new double[itemCount];
        double[] rating_items = new double[itemCount];
        double[] w_items = new double[itemCount];


        for (int i : itemList) {
            prediction_items[i] = predict(u, i);
            rating_items[i] = trainMatrix.getValue(u, i);
            w_items[i] = W.getValue(u, i);
        }

        DenseVector oldVector = U.row(u);
        if (weightStrategy != 2) {
            for (int f = 0; f < factors; f++) {
                double numer = 0, denom = 0;
                // O(K) complexity for the negative part
                for (int k = 0; k < factors; k++) {
                    if (k != f)
                        numer -= U.get(u, k) * SV.get(f, k);
                }
                //numer *= w0;

                // O(Nu) complexity for the positive part
                for (int i : itemList) {
                    prediction_items[i] -= U.get(u, f) * V.get(i, f);
                    numer += (w_items[i] * rating_items[i] - (w_items[i] - getNegWeight(i)) * prediction_items[i]) * V.get(i, f);
                    denom += (w_items[i] - getNegWeight(i)) * V.get(i, f) * V.get(i, f);
                }
                denom += SV.get(f, f) + reg;

                // Parameter Update
                U.set(u, f, numer / denom);

                // Update the prediction cache
                for (int i : itemList)
                    prediction_items[i] += U.get(u, f) * V.get(i, f);
            } // end for f

            // Update the SU cache
            for (int f = 0; f < factors; f++) {
                for (int k = 0; k <= f; k++) {
                    double val = SU.get(f, k) - oldVector.get(f) * oldVector.get(k)
                            + U.get(u, f) * U.get(u, k);
                    SU.set(f, k, val);
                    SU.set(k, f, val);
                }
            } // end for f
        }
        else {

            for (int f = 0; f < factors; f++) {
                // O(K) complexity for the w0 part
                double numer = 0, denom = 0;
                for (int k = 0; k < factors;  k ++) {
                    if (k != f)
                        numer -= U.get(u, k) * SV.get(f, k);
                }
                numer *= getNegWeight(u);

                // O(Ni) complexity for the positive ratings part
                for (int i : itemList) {
                    prediction_items[i] -= V.get(i, f) * U.get(u, f);
                    numer += (w_items[i]*rating_items[i] - (w_items[i]-getNegWeight(u)) * prediction_items[i]) * V.get(i, f);
                    denom += (w_items[i]-getNegWeight(u)) * V.get(i, f) * V.get(i, f);
                }
                denom += getNegWeight(u) * SV.get(f, f) + reg;

                // Parameter update
                U.set(u, f, numer / denom);
                // Update the prediction cache for the item
                for (int i : itemList)
                    prediction_items[i] += V.get(i, f) * U.get(u, f);
            } // end for f

            // Update the SV cache
            for (int f = 0; f < factors; f ++) {
                for (int k = 0; k <= f; k ++) {
                    double val = SU.get(f, k) - oldVector.get(f) * oldVector.get(k) * getNegWeight(u)
                            + U.get(u, f) * U.get(u, k) * getNegWeight(u);
                    SU.set(f, k, val);
                    SU.set(k, f, val);
                }
            }

        }
    }



    protected void update_item(int i) {
        ArrayList<Integer> userList = trainMatrix.getColRef(i).indexList();
        if (userList.size() == 0) return; // item has no ratings.
        // prediction cache for the item

        double[] prediction_users = new double[userCount];
        double[] rating_users = new double[userCount];
        double[] w_users = new double[userCount];

        for (int u : userList) {
            prediction_users[u] = predict(u, i);
            rating_users[u] = trainMatrix.getValue(u, i);
            w_users[u] = W.getValue(u, i);
        }

        DenseVector oldVector = V.row(i);
        if (weightStrategy != 2) {
            for (int f = 0; f < factors; f++) {
                // O(K) complexity for the w0 part
                double numer = 0, denom = 0;
                for (int k = 0; k < factors; k++) {
                    if (k != f)
                        numer -= V.get(i, k) * SU.get(f, k);
                }
                numer *= getNegWeight(i);

                // O(Ni) complexity for the positive ratings part
                for (int u : userList) {
                    prediction_users[u] -= U.get(u, f) * V.get(i, f);
                    numer += (w_users[u] * rating_users[u] - (w_users[u] - getNegWeight(i)) * prediction_users[u]) * U.get(u, f);
                    denom += (w_users[u] - getNegWeight(i)) * U.get(u, f) * U.get(u, f);
                }
                denom += getNegWeight(i) * SU.get(f, f) + reg;

                // Parameter update
                V.set(i, f, numer / denom);
                // Update the prediction cache for the item
                for (int u : userList)
                    prediction_users[u] += U.get(u, f) * V.get(i, f);
            } // end for f

            // Update the SV cache
            for (int f = 0; f < factors; f++) {
                for (int k = 0; k <= f; k++) {
                    double val = SV.get(f, k) - oldVector.get(f) * oldVector.get(k) * getNegWeight(i)
                            + V.get(i, f) * V.get(i, k) * getNegWeight(i);
                    SV.set(f, k, val);
                    SV.set(k, f, val);
                }
            }
        }
        else {

            for (int f = 0; f < factors; f++) {
                double numer = 0, denom = 0;
                // O(K) complexity for the negative part
                for (int k = 0; k < factors; k++) {
                    if (k != f)
                        numer -= V.get(i, k) * SU.get(f, k);
                }
                //numer *= w0;

                // O(Nu) complexity for the positive part
                for (int u : userList) {
                    prediction_users[u] -= V.get(i, f) * U.get(u, f);
                    numer += (w_users[u] * rating_users[u] - (w_users[u] - getNegWeight(u)) * prediction_users[u]) * U.get(u, f);
                    denom += (w_users[u] - getNegWeight(u)) * U.get(u, f) * U.get(u, f);
                }
                denom += SU.get(f, f) + reg;

                // Parameter Update
                V.set(i, f, numer / denom);

                // Update the prediction cache
                for (int u : userList)
                    prediction_users[u] += V.get(i, f) * U.get(u, f);
            } // end for f

            // Update the SU cache
            for (int f = 0; f < factors; f++) {
                for (int k = 0; k <= f; k++) {
                    double val = SV.get(f, k) - oldVector.get(f) * oldVector.get(k)
                            + V.get(i, f) * V.get(i, k);
                    SV.set(f, k, val);
                    SV.set(k, f, val);
                }
            } // end for f

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
        double L,l;
        if (weightStrategy != 2) {

            L = reg * (U.squaredSum() + V.squaredSum());
            for (int u = 0; u < userCount; u++) {
                l = 0;
                for (int i : trainMatrix.getRowRef(u).indexList()) {
                    double pred = predict(u, i);
                    l += W.getValue(u, i) * Math.pow(trainMatrix.getValue(u, i) - pred, 2);
                    l -= getNegWeight(i) * Math.pow(pred, 2);
                }
                l += SV.mult(U.row(u, false)).inner(U.row(u, false));
                L += l;
            }

            return L;
        }
        else {
            L = reg * (U.squaredSum() + V.squaredSum());
            for (int i = 0; i < itemCount; i++) {
                l = 0;
                for (int u : trainMatrix.getColRef(i).indexList()) {
                    double pred = predict(u, i);
                    l += W.getValue(u, i) * Math.pow(trainMatrix.getValue(u, i) - pred, 2);
                    l -= getNegWeight(u) * Math.pow(pred, 2);
                }
                l += SU.mult(V.row(i, false)).inner(V.row(i, false));
                L += l;
            }

            return L;
        }
    }

    /*
    public double[] loss() {
        double L = reg * (U.squaredSum() + V.squaredSum());
        double Lneg = 0;
        for (int u = 0; u < userCount; u ++) {
            double l = 0;
            for (int i : trainMatrix.getRowRef(u).indexList()) {
                double rating = trainMatrix.getValue(u, i);
                double predicted = predict(u, i);
                if (rating > 0) {
                    L += Math.pow(rating - predicted, 2);
                } else {
                    Lneg += getNegWeight(i) * Math.pow(rating - predicted, 2);
                }
            }
        }

        return 0.0;
    }
    */



    public double predict(int u, int i) {
        return U.row(u, false).inner(V.row(i, false));
    }


    public ArrayList<Integer> getSimilarItemsById(int id, int topN) {

       DenseVector a = V.mult(V.row(id,false));
       DenseVector itemNorms = new DenseVector(itemCount);
       for(int i=0; i < itemCount; i++) {
           itemNorms.set(i,Math.pow(V.row(id).squaredSum(),2));
       }

       return (a.divide(itemNorms)).topIndicesByValue(topN,null);

    }


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

    public DenseMatrix getUserFactors() {
        return this.U;
    }

    public DenseMatrix getItemFactors() { return this.V.transpose(); }       //need transpose?



    /*
    public void updateModel(int u, int i) {
        trainMatrix.setValue(u, i, 1);
        W.setValue(u, i, w_new);
        if (getNegWeight(i) == 0) { // an new item
            getNegWeight(i) = w0 / itemCount;
            // Update the SV cache
            for (int f = 0; f < factors; f ++) {
                for (int k = 0; k <= f; k ++) {
                    double val = SV.get(f, k) + V.get(i, f) * V.get(i, k) * getNegWeight(i);
                    SV.set(f, k, val);
                    SV.set(k, f, val);
                }
            }
        }


        for (int iter = 0; iter < maxIterOnline; iter ++) {
            update_user(u);

            update_item(i);
        }

    }
    */
    /*	// Raw way to calculate the loss function
    public double loss() {
        double L = reg * (U.squaredSum() + V.squaredSum());
        for (int u = 0; u < userCount; u ++) {
            double l = 0;
            for (int i : trainMatrix.getRowRef(u).indexList()) {
                l += Math.pow(trainMatrix.getValue(u, i) - predict(u, i), 2);
            }
            l *= (1 - w0);
            for (int i = 0; i < itemCount; i ++) {
                l += w0 * Math.pow(predict(u, i), 2);
            }
            L += l;
        }
        return L;
    } */
}

