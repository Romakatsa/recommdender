import algorithms.ItemPopularity;
import algorithms.MF_ALS;
import algorithms.MF_fastALS;
import data_structure.SparseMatrix;
import data_structure.SparseVector;
import utils.DataUtil;
import utils.Metrics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Created by Roma on 09.06.2017.
 */
public class Main {

    public static void main(String[] args) throws IOException {

        SparseMatrix ratingsMatrix = DataUtil.getTransposedRatingMatrix("choises0000_10000.ser",3000,10000);
        List<SparseMatrix> splittedMatrices = DataUtil.splitMatrix(ratingsMatrix, 0.1);
        List<String> artistsNames = DataUtil.getItemsList("artists0000_10000.txt",ratingsMatrix.length()[1]);

        //Add test user
        int[] testRockUserIds = new int[]{338,585,564,240,910,1296,2512,608,9293,3414,623,1290};    //rock
        int[] testRapUserIds = new int[]{338,585,564,240,910,1296,2512,608,9293,3414,623,1290};
        int[] testElecUserIds = new int[]{338,585,564,240,910,1296,2512,608,9293,3414,623,1290};
        SparseVector newUser = new SparseVector(ratingsMatrix.length()[1]);
        for (int i: testRockUserIds) {
            newUser.setValue(i,1);
        }
        splittedMatrices.get(0).setRowVector(2999, newUser);


        int factors = 20;
        int maxIter = 6;
        double defaultWeight = 200;
        double alpha = 0.5;
        double reg = 0.05;
        boolean showProgress = true;
        boolean showLoss = true;


        for (int fact=10; fact<150; fact += 15) {
            for (reg = 0.01; reg<= 10; reg = reg*10) {
                for (defaultWeight=50; defaultWeight<=300; defaultWeight += 50) {
                    for (alpha = 0; alpha <1; alpha += 0.1) {
                        MF_fastALS recommender = new MF_fastALS(splittedMatrices.get(0), factors, maxIter, defaultWeight, alpha, reg, showProgress, showLoss);
                        recommender.setWeights(1);  //uniformly
                        recommender.buildModel();
                        double aucScore = Metrics.evaluateAUC(splittedMatrices.get(0),splittedMatrices.get(1),recommender.getUserFactors(),recommender.getItemFactors(),splittedMatrices.get(1).getNonZeroRows());
                        DataUtil.writeCaseToFile("(Rock)eAls_f"+fact+"r"+reg+"w"+defaultWeight+"a"+alpha+"score"+(int)(aucScore*100),
                                recommender.getRecommendedItemsById(2999,15, true),artistsNames);
                        DataUtil.writeCaseToFile("(Rap)eAls_f"+fact+"r"+reg+"w"+defaultWeight+"a"+alpha+"score"+(int)(aucScore*100),
                                recommender.getRecommendedItemsById(2998,15, true),artistsNames);
                        DataUtil.writeCaseToFile("(Elec)eAls_f"+fact+"r"+reg+"w"+defaultWeight+"a"+alpha+"score"+(int)(aucScore*100),
                                recommender.getRecommendedItemsById(2997,15, true),artistsNames);

                    }
                }

                for (defaultWeight =0.01; defaultWeight<0.3; defaultWeight *= 2) {
                    MF_ALS recommender2 = new MF_ALS(splittedMatrices.get(0), factors, maxIter, defaultWeight, reg, showProgress, showLoss);
                    recommender2.buildModel();
                    double aucScore2 = Metrics.evaluateAUC(splittedMatrices.get(0),splittedMatrices.get(1),recommender2.getUserFactors(),recommender2.getItemFactors(),splittedMatrices.get(1).getNonZeroRows());
                    MF_fastALS recommender = new MF_fastALS(splittedMatrices.get(0), factors, maxIter, defaultWeight, alpha, reg, showProgress, showLoss);
                    recommender.setWeights(0);  //uniformly
                    recommender.buildModel();
                    double aucScore = Metrics.evaluateAUC(splittedMatrices.get(0),splittedMatrices.get(1),recommender.getUserFactors(),recommender.getItemFactors(),splittedMatrices.get(1).getNonZeroRows());

                }
            }
        }


        MF_fastALS recommender = new MF_fastALS(splittedMatrices.get(0), factors, maxIter, defaultWeight, alpha, reg, showProgress, showLoss);
        recommender.setWeights(1);
        recommender.buildModel();

        maxIter = 15;
        defaultWeight = 0.1;
        reg = 0.1;

       //MF_ALS recommender2 = new MF_ALS(splittedMatrices.get(0), factors, maxIter, defaultWeight, reg, showProgress, showLoss);
        //recommender2.buildModel();

        //ItemPopularity recommender3 = new ItemPopularity(splittedMatrices.get(0));
        //recommender3.buildModel();

        //double aucScore = Metrics.evaluateAUC(splittedMatrices.get(0),splittedMatrices.get(1),recommender.getUserFactors(),recommender.getItemFactors(),splittedMatrices.get(1).getNonZeroRows());


        //double aucScore2 = Metrics.evaluateAUC(splittedMatrices.get(0),splittedMatrices.get(1),recommender2.getUserFactors(),recommender2.getItemFactors(),splittedMatrices.get(1).getNonZeroRows());

        //double aucScore3 = Metrics.popAUC(splittedMatrices.get(0),splittedMatrices.get(1),recommender3.getItemPopularity(),splittedMatrices.get(1).getNonZeroRows());
        //System.out.println("eALS: " + aucScore);
        //System.out.println("ALS: " + aucScore2);
        //System.out.println("Pop: " + aucScore3);

        boolean exit = false;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in,"UTF-8"));
        String s;
        String partOfName;
        ArrayList<Integer> found;
        ArrayList<Integer> similar;
        int chosenId;

        similar = recommender.getRecommendedItemsById(2999,15, false);
        ArrayList<String> playlist = splittedMatrices.get(0).getRowRef(2999).indexList().stream().map(i->artistsNames.get(i)).collect(Collectors.toCollection(ArrayList::new));
        for (Integer id: similar) {
            System.out.println("ID: "+id+"  "+artistsNames.get(id));
        }
        System.out.println("----------------------------");
        for (String p: playlist) {
            System.out.println(p);
        }
        similar = recommender.getRecommendedItemsById(2999,15, true);
        for (Integer id: similar) {
            System.out.println("ID: "+id+"  "+artistsNames.get(id));
        }
        /*
        while (!exit) {
            System.out.print("Type part of name: ");
            s = br.readLine();

            if (s.equals("exit")) {
                    break;
            }
            found = DataUtil.getIdsByName(s,artistsNames);
            for (Integer id: found) {
                System.out.println("ID: "+id+"  Count: "+ ratingsMatrix.getColRef(id).itemCount() +" Name: "+ artistsNames.get(id));
            }


            System.out.println("Choose ID: ");
            s = br.readLine();
            try {
                chosenId = Integer.parseInt(s);
            }
            catch (NumberFormatException e) {
                continue;
            }
            similar = recommender.getSimilarItemsById(chosenId,15);
            for (Integer id: similar) {
                System.out.println("ID: "+id+"  "+artistsNames.get(id));
            }
            System.out.println("------------------------------------------");


        }
        */

    }

}
