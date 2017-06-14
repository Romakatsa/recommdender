import algorithms.ItemPopularity;
import algorithms.MF_ALS;
import algorithms.MF_fastALS;
import data_structure.SparseMatrix;
import data_structure.SparseVector;
import utils.DataUtil;
import utils.Metrics;

import javax.xml.crypto.Data;
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

        int[] testRockUserIds = new int[]{817,559,1141,1147,2130,2762,504,297,15};    //rock
        int[] testRapUserIds = new int[]{68,159,595,728,43,485,514,1974};
        int[] testElecUserIds = new int[]{729,839,569,658,1596,853,652};
        int[] testClassUserIds = new int[]{263,5953,4670,6526,4654,2051,2520,6175,336};
        //int[] testUkrUserIds = new int[]{199,125,4031,914,872,1600,237,244,242};
        int[] testJazzUserIds = new int[]{3111,6837,7426,6019,1016,1109};
        int[] testJazzRockUserIds = new int[]{3111,6837,7426,1141,1147,2130,2762};
        int[] testClassRockUserIds = new int[]{263,5953,4670,6526,817,559,1141,1147,2130};
        ArrayList<int[]> additionalUsers = new ArrayList<>();

        additionalUsers.add(testRockUserIds);
        additionalUsers.add(testElecUserIds);
        additionalUsers.add(testRapUserIds);
        additionalUsers.add(testClassUserIds);
        additionalUsers.add(testJazzUserIds);
        additionalUsers.add(testJazzRockUserIds);
        additionalUsers.add(testClassRockUserIds);

        int passLastN = additionalUsers.size();

        SparseMatrix ratingsMatrix = DataUtil.getTransposedRatingMatrix("choises0000_10000_v2.ser",additionalUsers);
        List<SparseMatrix> splittedMatrices = DataUtil.splitMatrix(ratingsMatrix, 0.15, passLastN);
        List<String> artistsNames = DataUtil.getItemsList("artists0000_10000_v2.txt",ratingsMatrix.length()[1]);


        //Add test user

        int totalUsers = ratingsMatrix.length()[0];
        /*
        int factors = 70;
        int maxIter = 10;
        double defaultWeight = 50;
        double alpha = 0.35;
        double reg = 0.005;
        boolean showProgress = true;
        boolean showLoss = true;
        */

        ArrayList<DataUtil.Settings> settingsList = DataUtil.getSettingsList("settings.txt");

        Long start = System.currentTimeMillis();
        //System.out.println("---------");

            /*
            for (reg = 0.005; reg<= 0.2; reg = reg*3) {
                for (defaultWeight=50; defaultWeight<=360; defaultWeight += 100) {
                    for (alpha = 0.15; alpha <0.7; alpha += 0.1) {
                    */
            for (DataUtil.Settings s:settingsList) {

                MF_fastALS recommender = new MF_fastALS(splittedMatrices.get(0),s.factors, s.maxIter, s.defaultWeight, s.alpha, s.reg, s.showProgress, s.showLoss);
                recommender.setWeights(1);  //uniformly
                recommender.buildModel();
                double aucScore = Metrics.evaluateAUC(splittedMatrices.get(0),splittedMatrices.get(1),recommender.getUserFactors(),recommender.getItemFactors(),splittedMatrices.get(1).getNonZeroRows());
                DataUtil.writeCaseToFile("(Rock)eAls_f"+s.factors+"r"+s.reg+"w"+s.defaultWeight+"a"+s.alpha+"score"+(int)(aucScore*1000),
                        recommender.getRecommendedItemsById(totalUsers-7,20, true),artistsNames);

                DataUtil.writeCaseToFile("(Elec)eAls_f"+s.factors+"r"+s.reg+"w"+s.defaultWeight+"a"+(int)(100*s.alpha)+"score"+(int)(aucScore*1000),
                        recommender.getRecommendedItemsById(totalUsers-6,20, true),artistsNames);

                DataUtil.writeCaseToFile("(Rap)eAls_f"+s.factors+"r"+s.reg+"w"+s.defaultWeight+"a"+(int)(100*s.alpha)+"score"+(int)(aucScore*1000),
                        recommender.getRecommendedItemsById(totalUsers-5,20, true),artistsNames);

                DataUtil.writeCaseToFile("(Class)eAls_f"+s.factors+"r"+s.reg+"w"+s.defaultWeight+"a"+(int)(100*s.alpha)+"score"+(int)(aucScore*1000),
                        recommender.getRecommendedItemsById(totalUsers-4,20, true),artistsNames);

                DataUtil.writeCaseToFile("(Jazz)eAls_f"+s.factors+"r"+s.reg+"w"+s.defaultWeight+"a"+(int)(100*s.alpha)+"score"+(int)(aucScore*1000),
                        recommender.getRecommendedItemsById(totalUsers-3,20, true),artistsNames);

                DataUtil.writeCaseToFile("(JazzRock)eAls_f"+s.factors+"r"+s.reg+"w"+s.defaultWeight+"a"+(int)(100*s.alpha)+"score"+(int)(aucScore*1000),
                        recommender.getRecommendedItemsById(totalUsers-2,20, true),artistsNames);

                DataUtil.writeCaseToFile("(ClassRock)eAls_f"+s.factors+"r"+s.reg+"w"+s.defaultWeight+"a"+(int)(100*s.alpha)+"score"+(int)(aucScore*1000),
                        recommender.getRecommendedItemsById(totalUsers-1,20, true),artistsNames);
            }


                        //DataUtil.writeCaseToFile("(Elec)eAls_f"+fact+"r"+reg+"w"+defaultWeight+"a"+alpha+"score"+(int)(aucScore*100),
                                //recommender.getRecommendedItemsById(2997,15, true),artistsNames);
                        //System.out.println("One iteration " + (System.currentTimeMillis() - start) + "millis");
                        //start = System.currentTimeMillis();
                    //}
                    //System.out.print("-");
                //}
                /*
                for (defaultWeight =0.01; defaultWeight<0.26; defaultWeight *= 2) {
                    MF_ALS recommender2 = new MF_ALS(splittedMatrices.get(0), factors, maxIter, defaultWeight, s.reg, showProgress, showLoss);
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

            //}


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

*/
        /*
        boolean exit = false;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in,"UTF-8"));
        String s;
        String partOfName;
        ArrayList<Integer> found;
        ArrayList<Integer> similar;
        int chosenId;


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



