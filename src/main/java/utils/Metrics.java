package utils;

import data_structure.DenseMatrix;
import data_structure.DenseVector;
import data_structure.SparseMatrix;
import data_structure.SparseVector;
import mloss.roc.Curve;
//import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by Roma on 07.06.2017.
 */
public class Metrics {

    public static double popAUC(SparseMatrix train, SparseMatrix test, double[] itemPopularity, Set<Integer> usersTaken) {

        int u = 0;
        int totalUsersTaken = usersTaken.size();
        double totalAucScore = 0;
        SparseVector userTrainChoises;
        SparseVector userTestChoises;
        DenseVector curUserFactors;
        double[] predictions;
        int[] actuals;
        Set<Integer> nonZeroIndices;
        Set<Integer> zeroIndices;
        HashSet<Integer> allIndices = IntStream.range(0, train.length()[1]).boxed().collect(Collectors.toCollection(HashSet::new));
        //System.out.println("Total users: "+totalUsersTaken);
        for (Integer user : usersTaken) {  //for those who where taken in trainset
            u++;

            userTrainChoises = train.getRowRef(user);
            userTestChoises = test.getRowRef(user);

            zeroIndices = (HashSet) allIndices.clone();
            zeroIndices.removeAll(userTrainChoises.indexSet());

            predictions = new double[zeroIndices.size()];
            int j = 0;
            for (int i : zeroIndices) {
                predictions[j] = itemPopularity[i];
                j++;
            }
            j = 0;
            actuals = new int[zeroIndices.size()];
            for (int i : zeroIndices) {
                actuals[j] = (int) userTestChoises.getValue(i);
                j++;
            }

            //predictions = zeroIndices.stream().mapToDouble(i -> curUserFactors.inner(itemsFactors.column(i))).toArray();
            //actuals = zeroIndices.stream().mapToInt(i -> (int) userTestChoises.getValue(i)).toArray();
            //predictions = IntStream.range(0,train.length()[1]).filter(i-> zeroIndices.contains(new Integer(i)))
            //.mapToDouble(k-> curUserFactors.inner(itemsFactors.column(k))).toArray();
            //zero_inds = np.where(user_choises == 0) #artists not listened to by user

            Curve analysis = new Curve.PrimitivesBuilder()
                    .predicteds(predictions)
                    .actuals(actuals)
                    .build();

            totalAucScore += analysis.rocArea();
            //analysis.rocPoints()
            if (u % 100 == 0) {
                System.out.println("AUC evaluated for " + u + " users");
            }
        }
        return totalAucScore / totalUsersTaken;


    }



    public static ArrayList<Curve> getUsersCurves(SparseMatrix train, SparseMatrix test, DenseMatrix usersFactors, DenseMatrix itemsFactors, Set<Integer> usersTaken) {


        int u = 0;
        int totalUsersTaken = usersTaken.size();
        double totalAucScore = 0;
        SparseVector userTrainChoises;
        SparseVector userTestChoises;
        DenseVector curUserFactors;
        double[] predictions;
        int[] actuals;
        Set<Integer> nonZeroIndices;
        Set<Integer> zeroIndices;
        HashSet<Integer> allIndices = IntStream.range(0, train.length()[1]).boxed().collect(Collectors.toCollection(HashSet::new));
        //System.out.println("Total users: "+totalUsersTaken);

        ArrayList<Curve> curves = new ArrayList<>(totalUsersTaken);

        for (Integer user : usersTaken) {  //for those who where taken in trainset
            u++;

            userTrainChoises = train.getRowRef(user);
            userTestChoises = test.getRowRef(user);
            curUserFactors = usersFactors.row(user);

            zeroIndices = (HashSet) allIndices.clone();
            zeroIndices.removeAll(userTrainChoises.indexSet());

            predictions = new double[zeroIndices.size()];
            int j = 0;
            for (int i : zeroIndices) {
                predictions[j] = (itemsFactors.column(i).inner(curUserFactors));
                j++;
            }
            j = 0;
            actuals = new int[zeroIndices.size()];
            for (int i : zeroIndices) {
                actuals[j] = (int) userTestChoises.getValue(i);
                j++;
            }

            //predictions = zeroIndices.stream().mapToDouble(i -> curUserFactors.inner(itemsFactors.column(i))).toArray();
            //actuals = zeroIndices.stream().mapToInt(i -> (int) userTestChoises.getValue(i)).toArray();
            //predictions = IntStream.range(0,train.length()[1]).filter(i-> zeroIndices.contains(new Integer(i)))
            //.mapToDouble(k-> curUserFactors.inner(itemsFactors.column(k))).toArray();
            //zero_inds = np.where(user_choises == 0) #artists not listened to by user

            Curve analysis = new Curve.PrimitivesBuilder()
                    .predicteds(predictions)
                    .actuals(actuals)
                    .build();
            curves.add(analysis);


        }

        return curves;
    }


    public static double[] evaluatePR_RECALL(SparseMatrix train, SparseMatrix test, DenseMatrix usersFactors, DenseMatrix itemsFactors, Set<Integer> usersTaken, int cutOff) {

        double[] PR_REC = new double[2];
        int totalUsersTaken = usersTaken.size();

        for (Curve c:getUsersCurves(train, test, usersFactors, itemsFactors, usersTaken)) {

            PR_REC[0] += c.precision(cutOff);
            PR_REC[1] += c.recall(cutOff);
        }

        PR_REC[0] /= totalUsersTaken;
        PR_REC[1] /= totalUsersTaken;
        return PR_REC;
    }


    public static double[] evaluateAll(SparseMatrix train, SparseMatrix test, DenseMatrix usersFactors, DenseMatrix itemsFactors, Set<Integer> usersTaken, int cutOff) {

        double[] PR_REC_AUC = new double[3];
        int totalUsersTaken = usersTaken.size();
        int u = 0;
        double totalAucScore = 0;
        SparseVector userTrainChoises;
        SparseVector userTestChoises;
        DenseVector curUserFactors;
        double[] predictions;
        int[] actuals;
        Set<Integer> nonZeroIndices;
        Set<Integer> zeroIndices;
        HashSet<Integer> allIndices = IntStream.range(0, train.length()[1]).boxed().collect(Collectors.toCollection(HashSet::new));
        //System.out.println("Total users: "+totalUsersTaken);

        //ArrayList<Curve> curves = new ArrayList<>(totalUsersTaken);

        for (Integer user : usersTaken) {  //for those who where taken in trainset
            u++;

            userTrainChoises = train.getRowRef(user);
            userTestChoises = test.getRowRef(user);
            curUserFactors = usersFactors.row(user);

            zeroIndices = (HashSet) allIndices.clone();
            zeroIndices.removeAll(userTrainChoises.indexSet());

            predictions = new double[zeroIndices.size()];
            int j = 0;
            for (int i : zeroIndices) {
                predictions[j] = (itemsFactors.column(i).inner(curUserFactors));
                j++;
            }
            j = 0;
            actuals = new int[zeroIndices.size()];
            for (int i : zeroIndices) {
                actuals[j] = (int) userTestChoises.getValue(i);
                j++;
            }

            //predictions = zeroIndices.stream().mapToDouble(i -> curUserFactors.inner(itemsFactors.column(i))).toArray();
            //actuals = zeroIndices.stream().mapToInt(i -> (int) userTestChoises.getValue(i)).toArray();
            //predictions = IntStream.range(0,train.length()[1]).filter(i-> zeroIndices.contains(new Integer(i)))
            //.mapToDouble(k-> curUserFactors.inner(itemsFactors.column(k))).toArray();
            //zero_inds = np.where(user_choises == 0) #artists not listened to by user

            Curve c = new Curve.PrimitivesBuilder()
                    .predicteds(predictions)
                    .actuals(actuals)
                    .build();

            PR_REC_AUC[0] += c.precision(cutOff);
            PR_REC_AUC[1] += c.recall(cutOff);
            PR_REC_AUC[2] += c.rocArea();

        }


        PR_REC_AUC[0] /= totalUsersTaken;
        PR_REC_AUC[1] /= totalUsersTaken;
        PR_REC_AUC[2] /= totalUsersTaken;
        return PR_REC_AUC;

    }

    public static double evaluateAUC(SparseMatrix train, SparseMatrix test, DenseMatrix usersFactors, DenseMatrix itemsFactors, Set<Integer> usersTaken) {

        int totalAucScore = 0;
        int totalUsersTaken = usersTaken.size();

        for (Curve c:getUsersCurves(train, test, usersFactors, itemsFactors, usersTaken)) {
            totalAucScore += c.rocArea();
        }

        return totalAucScore / totalUsersTaken;

    }


    public static double evaluatePrecision(SparseMatrix train, SparseMatrix test, DenseMatrix usersFactors, DenseMatrix itemsFactors, Set<Integer> usersTaken, int cutOff) {


        return 0;
    }

    public static double evaluateRecall(SparseMatrix train, SparseMatrix test, DenseMatrix usersFactors, DenseMatrix itemsFactors, Set<Integer> usersTaken, int cutOff) {


        return 0;
    }


}