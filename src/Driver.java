

import java.io.*;
import ttp.Optimisation.Optimisation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import ttp.TTPInstance;
import ttp.TTPSolution;
import ttp.Utils.DeepCopy;
import ttp.Utils.Utils;

/**
 *
 * @author wagner
 */
public class Driver {
    
    /* The current sequence of parameters is
     * args[0]  folder with TTP files
     * args[1]  pattern to identify the TTP problems that should be solved
     * args[2]  optimisation approach chosen
     * args[3]  stopping criterion: number of evaluations without improvement
     * args[4]  stopping criterion: time in milliseconds (e.g., 60000 equals 1 minute)
     * args[5]  opt parameter for the optimisation approach: mu
     * args[6]  opt parameter for the optimisation approach: lambda
     * args[7]  use preprocess 0: no, 1:yes
     */
    public static void main(String[] args) {
       
        if (args.length==0) 
//            args = new String[]{"instances", "a280_n1395_bounded-strongly-corr_", // to do all 10 instances (several files match the pattern)
//            args = new String[]{"instances", "a280_n1395_bounded-strongly-corr_10.ttp", // to do just this 1 instance
            args = new String[]{"instances", "fnl4461_n4460_bounded-strongly-corr_01.ttp", // to do just this 1 instance
//            args = new String[]{"instances", "pla33810_n338090_uncorr_10.ttp", // to do just this 1 instance
            "3", "1000000", "6000", "1", "1", "1"};
//        ttp.Optimisation.Optimisation.doAllLinkernTours();
//        runSomeTests();
        //generate_datas_preprocessing();
        generate_datas_mulambda();
        //doBatch(args);
    }

    public static void generate_datas_mulambda() {
        System.out.println("Start generating datas for the parameters:");

        double[][] results = new double[20][20];
        String to_print = "";

        for(int mu = 1; mu <= 3; mu++) {
            for(int lambda = 1; lambda <= 15; lambda++) {
                String[] args = new String[]{"instances", "fnl4461_n4460_bounded-strongly-corr_01.ttp", // to do just this 1 instance
//            args = new String[]{"instances", "pla33810_n338090_uncorr_10.ttp", // to do just this 1 instance
                        "2", "1000000", "2000", Integer.toString(mu), Integer.toString(lambda), "0"};
                System.out.println("Processing with mu="+mu+" lambda="+lambda);
                for(int j = 0; j < 10; j++ ) {
                    results[mu-1][lambda-1] += doBatch(args);
                }
                results[mu-1][lambda-1] /= 10;

                to_print += Double.toString(results[mu-1][lambda-1]);
                if(lambda != 15) to_print += ", ";
            }
            to_print += "\n";
        }

        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter("mu+lambda_"+System.currentTimeMillis(), false));
            writer.write(to_print);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void generate_datas_preprocessing() {
        System.out.println("Start generating datas for the preprocessing:");

        double[][] results = new double[8][2];

        for(int i = 1; i <= 8; i++) {
            System.out.println("Computing for duration " + Integer.toString (500*i));

            String[] args = new String[]{"instances", "fnl4461_n4460_bounded-strongly-corr_01.ttp", // to do just this 1 instance
//            args = new String[]{"instances", "pla33810_n338090_uncorr_10.ttp", // to do just this 1 instance
                    "1", "1000000", Integer.toString (500*i), "1", "1", "1"};

            for(int j = 0; j < 5; j++ ) {
                results[i-1][0] += doBatch(args);
            }
            results[i-1][0] /= 5;

            System.out.println("With no preproc:");

            args = new String[]{"instances", "fnl4461_n4460_bounded-strongly-corr_01.ttp", // to do just this 1 instance
//            args = new String[]{"instances", "pla33810_n338090_uncorr_10.ttp", // to do just this 1 instance
                    "1", "1000000", Integer.toString (500*i), "1", "1", "0"};

            for(int j = 0; j < 5; j++ ) {
                results[i-1][1] += doBatch(args);
            }
            results[i-1][1] /= 5;
        }

        /* prepare print */
        String to_print = "";
        for(int i = 0; i < 8; i++) {
            to_print += i + " " + results[i][0] + "\n";
        }

        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter("with_preproc"+System.currentTimeMillis(), false));
            writer.write(to_print);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        to_print = "";
        for(int i = 0; i < 8; i++) {
            to_print += i + " " + results[i][1] + "\n";
        }

        try {
            writer = new BufferedWriter(new FileWriter("no_preproc"+System.currentTimeMillis(), false));
            writer.write(to_print);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();

        }
    }
    
    // note: doBatch can process several files sequentially
    public static double doBatch(String[] args) {
//        String[] args = new String[]{"instances/","a2"};                      // first argument: folder with TTP and TSP files, second argument: partial filename of the instances to be solved   
//        System.out.println("parameters: "+Arrays.toString(args));
        File[] files = ttp.Utils.Utils.getFileList(args);
        
        int algorithm = Integer.parseInt(args[2]);
        int durationWithoutImprovement = Integer.parseInt(args[3]);
        int maxRuntime = Integer.parseInt(args[4]);

        int mu = 1;
        int lambda = 1;
        if(args.length >= 7) {
            mu = Integer.parseInt(args[5]);
            lambda = Integer.parseInt(args[6]);
        }
        int preproc = 1;
        if(args.length>= 8) {
            preproc = Integer.parseInt(args[7]);
        }
        
//        System.out.println("files.length="+files.length+" algorithm="+algorithm+" durationWithoutImprovement="+durationWithoutImprovement);
//        System.out.println("wend wendUsed fp ftraw ft ob computationTime");
        
        for (File f:files) {
            // read the TSP instance
            TTPInstance instance = new TTPInstance(f);
            
            long startTime = System.currentTimeMillis();
            String resultTitle = instance.file.getName() + ".NameOfTheAlgorithm." + startTime;
            
            // generate a Linkern tour (or read it if it already exists)
            int[] tour = Optimisation.linkernTour(instance);

            System.out.print(f.getName()+": ");
            
            // do the optimisation
            TTPSolution solution = Optimisation.hillClimber(instance, tour, algorithm, 
                    durationWithoutImprovement, maxRuntime, mu, lambda, preproc);
            
            
            // print to file
            solution.writeResult(resultTitle);
            
            // print to screen
            solution.println();
            
            return solution.getObjective();
//            solution.printFull();
        }

        return Double.MIN_VALUE;
    }
    
    
    public static void runSomeTests() {
        //        TTPInstance instance = new TTPInstance(new File("instances/a280_n279_bounded-strongly-corr_1.ttp"));
        TTPInstance instance = new TTPInstance(new File("instances/a280_n1395_bounded-strongly-corr_1.ttp"));
//        TTPInstance instance = new TTPInstance(new File("instances/a280_n2790_bounded-strongly-corr_10.ttp"));
//        TTPInstance instance = new TTPInstance(new File("instances/a280_n837_uncorr_9.ttp"));
//        instance.printInstance(false);
        
        int[] tour = new int[instance.numberOfNodes+1];
//        for (int i=0; i<tour.length; i++) tour[i] = i;
//        tour[instance.numberOfNodes]=0;
////        tour = permutation(tour.length);
        
        ttp.Utils.Utils.startTiming();
        tour = Optimisation.linkernTour(instance);
        ttp.Utils.Utils.stopTimingPrint();
        
        
        int[] packingPlan = new int[instance.numberOfItems];
        TTPSolution solution = new TTPSolution(tour, packingPlan);
        instance.evaluate(solution);
        System.out.print("\nLINKERN tour and no pickup: ");
        solution.printFull();
        
        packingPlan = new int[instance.numberOfItems];
        for (int i=0; i<packingPlan.length; i++) packingPlan[i] = 0;
//        for (int i=0; i<packingPlan.length; i++) packingPlan[i] = Math.random()<0.1?1:0;
        packingPlan[0]=1;
//        packingPlan[11]=1;
//        packingPlan[12]=1;
//        packingPlan[packingPlan.length-1]=1;
//        TTPSolution solution = new TTPSolution(tour, packingPlan);
//        instance.evaluate(solution);
//        solution.print();
        solution = new TTPSolution(tour, packingPlan);
        instance.evaluate(solution);
        System.out.print("\nLINKERN tour and only pickup of the first item: ");
        solution.printFull();
        
        int durationWithoutImprovement = 100;
        
        System.out.println("\nOptimiser: hillclimber (flip 1)");
        Optimisation.hillClimber(instance, tour, 1, durationWithoutImprovement, 600, 1, 1, 1).printFull();
        
        System.out.println("\nOptimiser: hillclimber (flip with prob 1/n)");
        Optimisation.hillClimber(instance, tour, 2, durationWithoutImprovement, 600, 1, 1, 1).printFull();
        
        
    }
    
    
}
