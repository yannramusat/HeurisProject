package ttp.Optimisation;


import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ttp.TTPInstance;
import ttp.TTPSolution;
import ttp.Utils.DeepCopy;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author wagner
 */
public class Optimisation {

    public static void preProcess(TTPInstance instance, int[] tour) {
    TTPSolution empty_sol=new TTPSolution(tour, new int[0]);
    instance.evaluate(empty_sol);
        for (int j = 0; j < instance.numberOfNodes; j++) {
            int i = 0;
            while (i < instance.items[j].length) {
                int[] array = new int[1];
                array[0] = instance.items[j][i];
                TTPSolution compare_sol = new TTPSolution(tour, array);
                instance.evaluate(compare_sol);
                if (compare_sol.fp < empty_sol.fp) {
                    System.arraycopy(instance.items[j], i + 1, instance.items[j], i, instance.items[j].length - 1 - i);
                    // delete item if it's no point taking it

                } else {
                    i++;
                }
            }
        }

    }

    /**
     * Computing binomiale law with Galton.
     * @param n
     * @param p
     * @return r in [0,n] correctly distributed
     */
    public static int binomiale(int n, double p) {
        int res = 0;
        for(int i = 0; i <= n; i++) {
            if(Math.random() < p) res++;
        }
        return res;
    }
    
    
    public static TTPSolution hillClimber(TTPInstance instance, int[] tour,
            int mode, 
            int durationWithoutImprovement, int maxRuntime, int mu, int lambda) {
        
        ttp.Utils.Utils.startTiming();
        //Optimisation.preProcess(instance,tour);

        boolean debugPrint = !true;

        if(mode == 1 || mode == 3) {
            mu = 1;
            lambda = 1;
        }
        //TTPSolution[] solutions = new TTPSolution[mu];
        int[][] packingPlans = new int[mu][instance.numberOfItems];
        //int[] packingPlan = new int[instance.numberOfItems];
        
                
        boolean improvement = true;
        double bestObjective = Double.NEGATIVE_INFINITY;

        double globalBestObjective = Double.NEGATIVE_INFINITY; // need one more for the simulated annealing
        TTPSolution s2 = null;
        
        long startingTimeForRuntimeLimit = System.currentTimeMillis()-200;
        
        int i = 0;
        int counter = 0;
        while(counter<durationWithoutImprovement) {
            
            if (i%10==0 /*do the time check just every 10 iterations, as it is time consuming*/
                    && (System.currentTimeMillis()-startingTimeForRuntimeLimit)>=maxRuntime)
                break;
            
            
            if (debugPrint) {
                System.out.println(" i="+i+"("+counter+") bestObjective="+bestObjective); 
            }

            //int[] newPackingPlan = (int[])DeepCopy.copy(packingPlan);
            int[][] newPackingPlans = new int[lambda][instance.numberOfItems];
            for(int k = 0; k < lambda; k++) {
                int position = (int)(Math.random()*mu);
                newPackingPlans[k] = (int[])DeepCopy.copy(packingPlans[position]);
            }
            
            boolean flippedToZero = false;
            
            switch (mode) {
                case 1:
                case 3: // simulated annealing
                    // flip one bit
                    int position = (int)(Math.random()*newPackingPlans[0].length);
//                    newPackingPlan[position] = Math.abs(newPackingPlan[position]-1);
                    if (newPackingPlans[0][position] == 1) {
                                newPackingPlans[0][position] = 0;
                                // investigation: was at least one item flipped to zero during an improvement?
//                                flippedToZero = true;
                    } else {
                        newPackingPlans[0][position] = 1;
                    }
                    break;
                case 2:
                case 4:
                    // flip with probability 1/n
                    for(int k = 0; k < lambda; k++) {
                        for (int j = 0; j < packingPlans[0].length; j++) {
                            if (Math.random() < 1d / packingPlans[0].length)
                                if (newPackingPlans[k][j] == 1) {
                                    newPackingPlans[k][j] = 0;
                                    // investigation: was at least one item flipped to zero during an improvement?
                                    //                                flippedToZero = true;
                                } else {
                                    newPackingPlans[k][j] = 1;
                                }
                        }
                    }
                    break;
                case 5:
                    int l = binomiale(packingPlans[0].length, lambda / packingPlans[0].length);
                    int[] positions = new int[l];
                    for(int ii = 0; ii < l; ii++)
                        positions[ii] = (int)(Math.random()*newPackingPlans[0].length);
                    for(int k = 0; k < lambda; k++) {
                        for(int ii = 0; ii < l; ii++) {
                            newPackingPlans[k][positions[ii]] = 1 - newPackingPlans[k][positions[ii]];
                        }
                    }
                    break;
            }
            
            
            
//            ttp.Utils.Utils.startTiming();
            //TTPSolution newSolution = new TTPSolution(tour, newPackingPlans[0]);
            //instance.evaluate(newSolution);

            TTPSolution[] newSolutions = new TTPSolution[lambda];
            for(int k = 0; k < lambda; k++) {
                newSolutions[k] = new TTPSolution(tour, newPackingPlans[k]);
                instance.evaluate(newSolutions[k]);
            }
//            System.out.println(ttp.Utils.Utils.stopTiming());

            if(mode == 2 || mode == 4) {
                //System.out.println();
                // test whether we have new optimum
                improvement = false;
                for(int k = 0; k < lambda; k++) {
                    if(newSolutions[k].ob > globalBestObjective && newSolutions[k].wend >= 0) {
                        counter = 0;
                        s2 = newSolutions[k].clone();
                        globalBestObjective = s2.ob;
                        improvement = true;
                        //System.out.println("New best seen: "+globalBestObjective);
                    }
                    //System.out.println("New seen: "+newSolutions[k].ob);
                }
                if(!improvement) counter++;

                // keep best ones
                for(int k = 0; k < mu; k++) {
                    int indice = 0;
                    double new_max = Double.NEGATIVE_INFINITY;
                    if(mode == 2) {
                        boolean lam = false;
                        for(int l = 0; l < lambda + mu; l++) {
                            if(l<mu) {
                                TTPSolution tmp = new TTPSolution(tour, packingPlans[l]);
                                instance.evaluate(tmp);
                                if(tmp.ob > new_max && tmp.wend >= 0) {
                                    new_max = tmp.ob;
                                    indice = l;
                                }
                            } else {
                                if(newSolutions[l-mu].ob > new_max && newSolutions[l-mu].wend >= 0) {
                                    new_max = newSolutions[l-mu].ob;
                                    indice = l-mu;
                                    lam = true;
                                }
                            }
                        }
                        if(lam) {
                            packingPlans[k] = (int[])DeepCopy.copy(newPackingPlans[indice]);
                            //System.out.println(solutions[k].ob);
                            newSolutions[indice].ob = Double.NEGATIVE_INFINITY;
                            //System.out.println(solutions[k].ob);
                        } else {
                            packingPlans[k] = (int[])DeepCopy.copy(packingPlans[indice]);
                        }
                    } else if (mode == 4) {
                        for(int l = 0; l < lambda; l++) {
                            if(newSolutions[l].ob > new_max && newSolutions[l].wend >= 0) {
                                new_max = newSolutions[l].ob;
                                indice = l;
                            }
                        }

                        packingPlans[k] = (int[])DeepCopy.copy(newPackingPlans[indice]);
                        //System.out.println(solutions[k].ob);
                        newSolutions[indice].ob = Double.NEGATIVE_INFINITY;
                        //System.out.println(solutions[k].ob);
                    }
                }

            } else if(mode == 1 || mode == 3) {
                /* replacement condition:
                 *   objective value has to be at least as good AND
                 *   the knapsack cannot be overloaded
                 */
                if (newSolutions[0].ob >= bestObjective && newSolutions[0].wend >= 0) {

                    // for the stopping criterion: check if there was an actual improvement
                    if (newSolutions[0].ob > bestObjective && newSolutions[0].wend >= 0) {
                        improvement = true;
                        counter = 0;
                    }

                    packingPlans[0] = newPackingPlans[0];
                    //s = newSolutions[0];
                    bestObjective = newSolutions[0].ob;

                    if (newSolutions[0].ob > globalBestObjective) {
                        s2 = newSolutions[0].clone();
                        globalBestObjective = newSolutions[0].ob;
                        //System.out.println("New best seen:"+globalBestObjective);
                    }
                    //System.out.println("New seen: "+newSolutions[0].ob);

                } else if (mode == 3 && newSolutions[0].wend >= 0) { // simulated annealing
                    /* proba */
                    double arg = (newSolutions[0].ob - bestObjective) * ttp.Utils.Utils.stopTiming();
                    if (Math.random() < Math.exp(arg)) {
                        /* update */
                        packingPlans[0] = newPackingPlans[0];
                        //s = newSolutions[0];
                        bestObjective = newSolutions[0].ob;
                    }
                    counter = 0; // relaunch the counter to avoid blocking
                } else {
                    improvement = false;
                    counter++;
                }
            } else if (mode == 5) {
                int[] optPackingPlan = new int[newPackingPlans[0].length];
                double maxobj = 0;
                for(int ii = 0; ii < lambda; ii++) {
                    if (newSolutions[ii].ob > maxobj && newSolutions[ii].wend >= 0) {
                        optPackingPlan = newSolutions[ii].packingPlan;
                        maxobj = newSolutions[ii].ob;
                    }
                }
                maxobj = 0;
                TTPSolution newSol = null;
                for(int k = 0; k < lambda; k++) {
                    for(int ii = 0; ii < newPackingPlans[0].length; ii++) {
                        if (Math.random() < 1/lambda) newPackingPlans[k][ii] = s2.packingPlan[ii];
                                else newPackingPlans[k][ii] = optPackingPlan[ii];
                    }
                    newSol = new TTPSolution(tour, newPackingPlans[k]);
                    instance.evaluate(newSol);
                    if(newSol.ob > maxobj && newSol.wend >= 0) {
                        optPackingPlan = newSol.packingPlan;
                        maxobj = newSol.ob;
                    }
                }
                if (newSol.ob > globalBestObjective) {
                    counter = 0;
                    s2 = newSol.clone();
                    globalBestObjective = newSol.ob;
                }
            }

            i++;
            
        }
        
        long duration = ttp.Utils.Utils.stopTiming();
        s2.computationTime = duration;
        return s2;
    }
    
    public static int[] linkernTour(TTPInstance instance) {
        int[] result = new int[instance.numberOfNodes+1];
        
        boolean debugPrint = !true;
        
        String temp = instance.file.getAbsolutePath();
        int index = temp.indexOf("_");
        String tspfilename = temp;//.substring(0,index)+".tsp";
        if (index==-1) index = tspfilename.indexOf(".");
        String tspresultfilename = temp.substring(0,index)+".linkern.tour";
        
        if (debugPrint) System.out.println("LINKERN: "+tspfilename);
    
        File tspresultfile = new File(tspresultfilename);
        
        
        try {
            if (!tspresultfile.exists()) {
                List<String> command = new ArrayList<String>();
                command.add("./linkern");
                command.add("-o");
                command.add(tspresultfilename);
                command.add(tspfilename);
//                printListOfStrings(command);

                ProcessBuilder builder = new ProcessBuilder(command);
                builder.redirectErrorStream(true);
                final Process process = builder.start();
                InputStream is = process.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line;
                while ((line = br.readLine()) != null) {
                    if (debugPrint) System.out.println("<LINKERN> "+line);
                }
                if (debugPrint) System.out.println("Program terminated?");    
                int rc = process.waitFor();
                if (debugPrint) System.out.println("Program terminated!");
            }

            List<String> command = new ArrayList<String>();
            command.add("cat");
            command.add(tspresultfilename);
//            printListOfStrings(command);
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            final Process process = builder.start();
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            // discard the first line
            String line = br.readLine();                    
            for (int i=0; i<result.length; i++) {
                line = br.readLine();
                if (debugPrint) System.out.println("<TOUR> "+line);
                index = line.indexOf(" ");
                int number = Integer.parseInt(line.substring(0,index));
                result[i] = number;
                if (debugPrint) System.out.println(Arrays.toString(result));
            }
            if (debugPrint) System.out.println("Program terminated?");    
            int rc = process.waitFor();
            if (debugPrint) System.out.println("Program terminated!");
            
            } catch (Exception ex) {
            }
        return result;
    }
    
    public static void doAllLinkernTours() {
        
        boolean debugPrint = false;
        
        File f = new File("instances/tsplibCEIL");
//        File f = new File("instances/");
        try {
            if (debugPrint) System.out.println(f.getCanonicalPath());
        } catch (IOException ex) {
        }
        
        File[] fa = f.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                boolean result = false;
//                if (name.contains(".ttp") 
                if (name.contains(".tsp") 
                        ) result = true;
                return result;
            }});
        
        if (debugPrint)
            for (File temp:fa) {
                System.out.println(temp.getAbsolutePath());
            }
        
        // create a nonsense instance just to be able to run linkernTour/1 on it
//        TTPInstance instance = new TTPInstance(new File("."));        
//        int[] tour = new int[0];
//        tour = Optimisation.linkernTour(instance);
        
        
        
//        int[] result = new int[instance.numberOfNodes+1];
//        
//        boolean debugPrint = !true;
//        
//        String temp = instance.file.getAbsolutePath();
//        int index = temp.indexOf("_");
        for(File tsp:fa) {
            String tspfilename = tsp.getAbsolutePath();
            int index = tspfilename.indexOf("_");
            if (index==-1) index = tspfilename.indexOf(".");
            String tspresultfilename = tspfilename.substring(0, index) +".linkern.tour";
//            int index = tspfilename.indexOf(".tsp");
//            String tspresultfilename = tspfilename.substring(0, index) +".linkern.tour";
//            String tspresultfilename = tspfilename+".linkern.tour";

            if (debugPrint) System.out.println("LINKERN: "+tspfilename);

            File tspresultfile = new File(tspresultfilename);

            try {
                if (! tspresultfile.exists()) {
                    List<String> command = new ArrayList<String>();
                    command.add("./linkern");
                    command.add("-o");
                    command.add(tspresultfilename);
                    command.add(tspfilename);
//                    printListOfStrings(command);

                    ProcessBuilder builder = new ProcessBuilder(command);
                    builder.redirectErrorStream(true);
                    
                    ttp.Utils.Utils.startTiming();
                    
                    final Process process = builder.start();
                    InputStream is = process.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader br = new BufferedReader(isr);
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (debugPrint) System.out.println("<LINKERN> "+line);
                    }
                    if (debugPrint) System.out.println("Program terminated?");    
                    int rc = process.waitFor();
                    
                    long duration = ttp.Utils.Utils.stopTiming();
                    
                    System.out.println( new File(tspresultfilename).getName() +" "+duration);
                    
                    if (debugPrint) System.out.println("Program terminated!");
                }
                
                
                
                
                } catch (Exception ex) {
                }
        }
        
    }
    
    public static void printListOfStrings(List<String> list) {
        String result = "";
        for (String s:list)
            result+=s+" ";
        System.out.println(result);
    }
}
