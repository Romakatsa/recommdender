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

        SparseMatrix ratingsMatrix = DataUtil.getTransposedRatingMatrix("choises0000_10000_v2.ser");
        List<SparseMatrix> splittedMatrices = DataUtil.splitMatrix(ratingsMatrix, 0.1);
        List<String> artistsNames = DataUtil.getItemsList("artists0000_10000_v2.txt",ratingsMatrix.length()[1]);

        //Add test user
        int[] testRockUserIds = new int[]{817,559,1141,1147,2130,2762,504,297,15};    //rock
        int[] testRapUserIds = new int[]{68,159,595,728,43,485,514,1974};
        int[] testElecUserIds = new int[]{729,839,569,658,1596,853,652};
        int[] testClassUserIds = new int[]{263,5953,4670,6526,4654,2051,2520,6175,336};
        int[] testUkrUserIds = new int[]{199,125,4031,914,872,1600,237,244,242};
        SparseVector newRockUser = new SparseVector(ratingsMatrix.length()[1]);
        SparseVector newElecUser = new SparseVector(ratingsMatrix.length()[1]);
        SparseVector newRapUser = new SparseVector(ratingsMatrix.length()[1]);
        SparseVector newClassUser = new SparseVector(ratingsMatrix.length()[1]);
        SparseVector newUkrUser = new SparseVector(ratingsMatrix.length()[1]);
        for (int i: testRockUserIds) {
            newRockUser.setValue(i,1);
        }
        for (int i: testElecUserIds) {
            newElecUser.setValue(i,1);
        }
        for (int i: testRapUserIds) {
            newRapUser.setValue(i,1);
        }
        for (int i: testClassUserIds) {
            newClassUser.setValue(i,1);
        }
        for (int i: testUkrUserIds) {
            newUkrUser.setValue(i,1);
        }
        int totalUsers = ratingsMatrix.length()[0];
        splittedMatrices.get(0).setRowVector(totalUsers-1, newRockUser);
        splittedMatrices.get(0).setRowVector(totalUsers-2, newElecUser);
        splittedMatrices.get(0).setRowVector(totalUsers-3, newRapUser);
        splittedMatrices.get(0).setRowVector(totalUsers-4, newClassUser);
        splittedMatrices.get(0).setRowVector(totalUsers-5, newUkrUser);

        int factors = 70;
        int maxIter = 10;
        double defaultWeight = 200;
        double alpha = 0.5;
        double reg = 0.05;
        boolean showProgress = false;
        boolean showLoss = false;

        Long start = System.currentTimeMillis();
        System.out.println("---------");


            for (reg = 0.005; reg<= 0.2; reg = reg*3) {
                for (defaultWeight=50; defaultWeight<=360; defaultWeight += 100) {
                    for (alpha = 0.15; alpha <0.7; alpha += 0.1) {
                        MF_fastALS recommender = new MF_fastALS(splittedMatrices.get(0), factors, maxIter, defaultWeight, alpha, reg, showProgress, showLoss);
                        recommender.setWeights(1);  //uniformly
                        recommender.buildModel();
                        double aucScore = Metrics.evaluateAUC(splittedMatrices.get(0),splittedMatrices.get(1),recommender.getUserFactors(),recommender.getItemFactors(),splittedMatrices.get(1).getNonZeroRows());
                        DataUtil.writeCaseToFile("(Rock)eAls_f"+factors+"r"+reg+"w"+defaultWeight+"a"+alpha+"score"+(int)(aucScore*1000),
                                recommender.getRecommendedItemsById(totalUsers-1,20, true),artistsNames);

                        DataUtil.writeCaseToFile("(Elec)eAls_f"+factors+"r"+reg+"w"+defaultWeight+"a"+alpha+"score"+(int)(aucScore*1000),
                                recommender.getRecommendedItemsById(totalUsers-2,20, true),artistsNames);

                        DataUtil.writeCaseToFile("(Rap)eAls_f"+factors+"r"+reg+"w"+defaultWeight+"a"+alpha+"score"+(int)(aucScore*1000),
                                recommender.getRecommendedItemsById(totalUsers-3,20, true),artistsNames);

                        DataUtil.writeCaseToFile("(Class)eAls_f"+factors+"r"+reg+"w"+defaultWeight+"a"+alpha+"score"+(int)(aucScore*1000),
                                recommender.getRecommendedItemsById(totalUsers-4,20, true),artistsNames);

                        DataUtil.writeCaseToFile("(Ukr)eAls_f"+factors+"r"+reg+"w"+defaultWeight+"a"+alpha+"score"+(int)(aucScore*1000),
                                recommender.getRecommendedItemsById(totalUsers-5,20, true),artistsNames);

                        //DataUtil.writeCaseToFile("(Elec)eAls_f"+fact+"r"+reg+"w"+defaultWeight+"a"+alpha+"score"+(int)(aucScore*100),
                                //recommender.getRecommendedItemsById(2997,15, true),artistsNames);
                        //System.out.println("One iteration " + (System.currentTimeMillis() - start) + "millis");
                        //start = System.currentTimeMillis();
                    }
                    System.out.print("-");
                }
                /*
                for (defaultWeight =0.01; defaultWeight<0.26; defaultWeight *= 2) {
                    MF_ALS recommender2 = new MF_ALS(splittedMatrices.get(0), factors, maxIter, defaultWeight, reg, showProgress, showLoss);
                    recommender2.buildModel();
                    double aucScore = Metrics.evaluateAUC(splittedMatrices.get(0),splittedMatrices.get(1),recommender2.getUserFactors(),recommender2.getItemFactors(),splittedMatrices.get(1).getNonZeroRows());
                    DataUtil.writeCaseToFile("(Rock)Als_f"+fact+"r"+reg+"w"+defaultWeight+"score"+(int)(aucScore*100),
                            recommender2.getRecommendedItemsById(totalUsers-1,20, true),artistsNames);
                    DataUtil.writeCaseToFile("(Elec)Als_f"+fact+"r"+reg+"w"+defaultWeight+"score"+(int)(aucScore*100),
                            recommender2.getRecommendedItemsById(totalUsers-2,20, true),artistsNames);
                    DataUtil.writeCaseToFile("(Rock)Als_f"+fact+"r"+reg+"w"+defaultWeight+"score"+(int)(aucScore*100),
                            recommender2.getRecommendedItemsById(totalUsers-1,20, false),artistsNames);
                    DataUtil.writeCaseToFile("(Elec)Als_f"+fact+"r"+reg+"w"+defaultWeight+"score"+(int)(aucScore*100),
                            recommender2.getRecommendedItemsById(totalUsers-2,20, false),artistsNames);
                }
                */

            }


        /*
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
