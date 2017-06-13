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
        System.out.println("Total users: "+totalUsersTaken);
        for (Integer user : usersTaken) {  //for those who where taken in trainset
            u++;

            userTrainChoises = train.getRowRef(user);
            userTestChoises = test.getRowRef(user);

            zeroIndices = (HashSet) allIndices.clone();
            zeroIndices.removeAll(userTrainChoises.indexSet());

            predictions = new double[zeroIndices.size()];
            int j = 0;
            for (int i: zeroIndices) {
                predictions[j] = itemPopularity[i];
                j++;
            }
            j = 0;
            actuals = new int[zeroIndices.size()];
            for (int i: zeroIndices) {
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
            if (u%100 == 0) {
                System.out.println("AUC evaluated for "+u+" users");
            }
        }
        return totalAucScore/totalUsersTaken;



    }


    public static double evaluateAUC(SparseMatrix train, SparseMatrix test, DenseMatrix usersFactors, DenseMatrix itemsFactors, Set<Integer> usersTaken) {

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
        System.out.println("Total users: "+totalUsersTaken);
        for (Integer user : usersTaken) {  //for those who where taken in trainset
            u++;

            userTrainChoises = train.getRowRef(user);
            userTestChoises = test.getRowRef(user);
            curUserFactors = usersFactors.row(user);

            zeroIndices = (HashSet) allIndices.clone();
            zeroIndices.removeAll(userTrainChoises.indexSet());

            predictions = new double[zeroIndices.size()];
            int j = 0;
            for (int i: zeroIndices) {
                predictions[j] = (itemsFactors.column(i).inner(curUserFactors));
                j++;
            }
            j = 0;
            actuals = new int[zeroIndices.size()];
            for (int i: zeroIndices) {
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
            if (u%100 == 0) {
                System.out.println("AUC evaluated for "+u+" users");
            }
        }
        return totalAucScore/totalUsersTaken;


    }

}
