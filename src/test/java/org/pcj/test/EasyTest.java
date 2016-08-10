/* 
 * Copyright (c) 2016, Marek Nowicki
 * All rights reserved.
 * 
 * Licensed under New BSD License (3-clause license).
 * 
 * See the file "LICENSE" for the full license governing this code.
 */
package org.pcj.test;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pcj.Group;
import org.pcj.NodesDescription;
import org.pcj.PCJ;
import org.pcj.Shared;
import org.pcj.StartPoint;

/**
 *
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
public class EasyTest implements StartPoint {

    enum SharedEnum implements Shared {
        a(double.class),
        b(double[].class),
        c(Double.class);
        private final Class<?> clazz;

        private SharedEnum(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public Class<?> type() {
            return clazz;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Level level = Level.INFO;
//        Level level = Level.FINEST;
        Logger logger = Logger.getLogger("");
        Arrays.stream(logger.getHandlers()).forEach(handler -> handler.setLevel(level));
        logger.setLevel(level);

        NodesDescription nodesDescription = new NodesDescription(new String[]{
            "localhost:8091",
            "localhost:8091", "localhost:8091", "localhost:8091", "localhost:8091", "localhost:8091", "localhost:8091", "localhost:8091", "localhost:8091", "localhost:8091",
            "localhost:8002",
            "localhost:8002", "localhost:8002", "localhost:8002", "localhost:8002", "localhost:8002", "localhost:8002", "localhost:8002", "localhost:8002", "localhost:8002",
            "localhost:8003",
            "localhost:8003", "localhost:8003", "localhost:8003", "localhost:8003", "localhost:8003", "localhost:8003", "localhost:8003", "localhost:8003", "localhost:8003",
            "localhost:8004",
            "localhost:8004", "localhost:8004", "localhost:8004", "localhost:8004", "localhost:8004", "localhost:8004", "localhost:8004", "localhost:8004", "localhost:8004",
            "localhost:8005",
            "localhost:8005", "localhost:8005", "localhost:8005", "localhost:8005", "localhost:8005", "localhost:8005", "localhost:8005", "localhost:8005", "localhost:8005",
            "localhost:8006",
            "localhost:8006", "localhost:8006", "localhost:8006", "localhost:8006", "localhost:8006", "localhost:8006", "localhost:8006", "localhost:8006", "localhost:8006",
            "localhost:8007",
            "localhost:8007", "localhost:8007", "localhost:8007", "localhost:8007", "localhost:8007", "localhost:8007", "localhost:8007", "localhost:8007", "localhost:8007",
            "localhost:8008",
            "localhost:8008", "localhost:8008", "localhost:8008", "localhost:8008", "localhost:8008", "localhost:8008", "localhost:8008", "localhost:8008", "localhost:8008",
            "localhost:8009",
            "localhost:8009", "localhost:8009", "localhost:8009", "localhost:8009", "localhost:8009", "localhost:8009", "localhost:8009", "localhost:8009", "localhost:8009", //
            // run.jvmargs=-Xmx64m
            "localhost:8010",
            "localhost:8010", "localhost:8010", "localhost:8010", "localhost:8010", "localhost:8010", "localhost:8010", "localhost:8010", "localhost:8010", "localhost:8010",
            "localhost:8011",
            "localhost:8011", "localhost:8011", "localhost:8011", "localhost:8011", "localhost:8011", "localhost:8011", "localhost:8011", "localhost:8011", "localhost:8011",
            "localhost:8012",
            "localhost:8012", "localhost:8012", "localhost:8012", "localhost:8012", "localhost:8012", "localhost:8012", "localhost:8012", "localhost:8012", "localhost:8012",
            "localhost:8013",
            "localhost:8013", "localhost:8013", "localhost:8013", "localhost:8013", "localhost:8013", "localhost:8013", "localhost:8013", "localhost:8013", "localhost:8013",
            "localhost:8014",
            "localhost:8014", "localhost:8014", "localhost:8014", "localhost:8014", "localhost:8014", "localhost:8014", "localhost:8014", "localhost:8014", "localhost:8014",
            "localhost:8015",
            "localhost:8015", "localhost:8015", "localhost:8015", "localhost:8015", "localhost:8015", "localhost:8015", "localhost:8015", "localhost:8015", "localhost:8015",
            "localhost:8016",
            "localhost:8016", "localhost:8016", "localhost:8016", "localhost:8016", "localhost:8016", "localhost:8016", "localhost:8016", "localhost:8016", "localhost:8016",
            "localhost:8017",
            "localhost:8017", "localhost:8017", "localhost:8017", "localhost:8017", "localhost:8017", "localhost:8017", "localhost:8017", "localhost:8017", "localhost:8017",
            "localhost:8018",
            "localhost:8018", "localhost:8018", "localhost:8018", "localhost:8018", "localhost:8018", "localhost:8018", "localhost:8018", "localhost:8018", "localhost:8018",
            "localhost:8019",
            "localhost:8019", "localhost:8019", "localhost:8019", "localhost:8019", "localhost:8019", "localhost:8019", "localhost:8019", "localhost:8019", "localhost:8019",
        });

        PCJ.deploy(EasyTest.class, nodesDescription, SharedEnum.class);
    }

    @Override
    public void main() throws Throwable {
//        Thread.sleep((PCJ.getNodeCount()- PCJ.getNodeId()) * 50);

        Group g = PCJ.join("group" + (PCJ.myId() % 2));
        PCJ.barrier();

        for (int i = 0; i < g.threadCount(); ++i) {
            if (g.myId() == i) {
                System.out.println(g.getGroupName() + ">>> global: " + PCJ.myId() + " group:" + g.myId() + "/" + g.threadCount());
            }
            g.asyncBarrier().get();
        }
        PCJ.barrier();

        for (int i = 1; i <= 500; ++i) {
            Thread.sleep((long) (Math.random() * 100));
            System.out.println(PCJ.myId() + "> joining to test" + i);
            PCJ.join("test" + i);
            System.out.println(PCJ.myId() + "> joined to test" + i);
//            if (i % 20 == 0) {
            PCJ.barrier();
//            }
        }
        PCJ.barrier();
//        System.out.println(PCJ.myId() + "> DONE");
//
////        Level level = Level.FINEST;
////        Logger logger = Logger.getLogger("");
////        Arrays.stream(logger.getHandlers()).forEach(handler -> handler.setLevel(level));
////        logger.setLevel(level);
////        for (int i = 0; i < PCJ.threadCount(); ++i) {
////            if (PCJ.myId() == i) {
////                System.out.println("Starting as " + PCJ.myId());
////            }
////            PCJ.barrier();
////        }
////
////        int n = 4*8*8192;
//////        int n = 4096;
////
//////        if (PCJ.myId() == 1) {
//////            double[] b = new double[n];
//////            for (int i = 0; i < n; i++) {
//////                b[i] = (double) i + 1;
//////            }
//////
//////            PCJ.putLocal(SharedEnum.b, b);
//////        }
//////        PCJ.barrier();
//////        if (PCJ.myId() == 0) {
//////            PCJ.get(1, SharedEnum.b);
//////        }
////        double[] b = new double[n];
////        for (int i = 0; i < n; i++) {
////            b[i] = (double) i + 1;
////        }
////        PCJ.monitor(SharedEnum.b);
////
////        PCJ.barrier();
////
////        int ntimes = 100;
////
////        for (int i = 0; i < ntimes; i++) {
////            if (PCJ.myId() == 0) {
////                try {
////                    PCJ.broadcast(SharedEnum.b, b);
////                } catch (Exception ex) {
////                    System.out.println(ex.getMessage());
////                }
////            }
//////            PCJ.waitFor(SharedEnum.b);
////            PCJ.barrier();
////        }
////        PCJ.barrier();
////        System.out.println(PCJ.myId() + " -> " + Arrays.toString((double[]) PCJ.getLocal(SharedEnum.b)));
    }
}
