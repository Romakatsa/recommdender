package utils;

import data_structure.SparseMatrix;
import data_structure.SparseVector;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Created by Roma on 09.06.2017.
 */
public class DataUtil {



    public static ArrayList<Integer> getIdsByName(String name, List<String> items) {

        ArrayList<Integer> ids = new ArrayList<>();
        for(int i =0; i< items.size(); i++) {

            if (items.get(i).toLowerCase().contains(name.toLowerCase())) {
                ids.add(i);
            }

        }

        return ids;
    }


    public static List<String> getItemsList(String filename, int rows) throws IOException {

        ArrayList<String> items = new ArrayList<>(rows);

        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filename).getAbsoluteFile()), "UTF-8"));


        try (LineNumberReader lnr = new LineNumberReader(br)) {
            for (String line; (line = lnr.readLine()) != null; ) {

                if (lnr.getLineNumber() > rows) {
                    break;
                }
                items.add(line);
            }
        }

        return items;

    }

    public static SparseMatrix getTransposedRatingMatrix(String filename) {

        byte[][] ratings2DArray = (byte[][]) deserealizeObj(filename);
        SparseMatrix ratings = new SparseMatrix(ratings2DArray[0].length, ratings2DArray.length);

        for (int i = 0;i<ratings2DArray.length;i++) {
            for (int j=0;j<ratings2DArray[0].length;j++) {
                if (ratings2DArray[i][j] != 0)
                ratings.setValue(j,i,1);
            }
        }


        return ratings;
    }


    public static void writeCaseToFile(String filename, ArrayList<Integer> recs, List<String> items) {

        PrintWriter f = null;
        try {
            f = new PrintWriter(new FileWriter(filename,true));
        } catch (IOException e) {
            e.printStackTrace();
        }

        for(Integer rec: recs) {
            f.println("ID: "+rec +" Name: "+items.get(rec));
        }
        f.println("-------------------------------------------------");
        f.close();

    }


    public static SparseMatrix getTransposedRatingMatrix(String filename, int rows, int cols) {

        byte[][] ratings2DArray = (byte[][]) deserealizeObj(filename);
        SparseMatrix ratings = new SparseMatrix(rows, cols);

        for (int i = 0;i<cols;i++) {
            for (int j=0;j<rows;j++) {
                if (ratings2DArray[i][j] != 0)
                    ratings.setValue(j,i,1);
            }
        }

        return ratings;
    }



    public static List<SparseMatrix> splitMatrix(SparseMatrix ratings, double testPortion) {

        boolean userTaken = false;

        List<SparseMatrix> samples = new ArrayList<>(2);
        SparseMatrix trainset;
        SparseMatrix testset;

        trainset = new SparseMatrix(ratings.length()[0],ratings.length()[1]);
        testset = new SparseMatrix(ratings.length()[0],ratings.length()[1]);


        for (int u =0;u < ratings.length()[0]; u++) {
            SparseVector row = ratings.getRowRef(u);
            for (int i : row.indexList()) {

                if (Math.random() < testPortion) {
                    testset.setValue(u,i,1);
                    userTaken = true;
                }
                else {
                    trainset.setValue(u,i,1);
                }

            }
            if (userTaken) {
                testset.setRowNonZero(u);
                userTaken = false;
            }

        }
        samples.add(trainset);
        samples.add(testset);

        return samples;
    }


    public static Object deserealizeObj(String file_name) {

        Object obj = null;
        try
        {
            FileInputStream inputFileStream = new FileInputStream(file_name);
            ObjectInputStream objectInputStream = new ObjectInputStream(inputFileStream);
            obj  = objectInputStream.readObject();
            objectInputStream.close();
            inputFileStream.close();
        }
        catch(ClassNotFoundException e)
        {
            e.printStackTrace();
        }
        catch(IOException i)
        {
            i.printStackTrace();
        }
        return obj;

    }

    public static void serealizeObj(Object obj,String file_name) throws IOException {

        FileOutputStream pairs_out = new FileOutputStream(file_name);
        ObjectOutputStream pairs_oos = new ObjectOutputStream(pairs_out);
        pairs_oos.writeObject(obj);
        pairs_oos.close();

    }



}