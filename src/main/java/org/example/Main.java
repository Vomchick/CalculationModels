package org.example;

public class Main {
    public static void main(String[] args) {
        Controller ct1 = new Controller("Model");
        ct1.readDataFrom(dataDir + "data1.txt").runModel();
        String res = ct1.getResultsAsTsv();
        System.out.println(res);
    }
}