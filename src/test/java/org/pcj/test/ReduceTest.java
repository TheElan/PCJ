/*
 * Copyright (c) 2011-2019, PCJ Library, Marek Nowicki
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
import org.pcj.NodesDescription;
import org.pcj.PCJ;
import org.pcj.RegisterStorage;
import org.pcj.StartPoint;
import org.pcj.Storage;

/**
 * @author Marek Nowicki (faramir@mat.umk.pl)
 */
@RegisterStorage(ReduceTest.Communicable.class)
public class ReduceTest implements StartPoint {

    @Storage(ReduceTest.class)
    enum Communicable {
        intValue,
        doubleArray
    }

    private int intValue;
    private double[] doubleArray = new double[1];

    public static void main(String[] args) {
        Level level = Level.INFO;
//        Level level = Level.FINEST;
        Logger logger = Logger.getLogger("");
        Arrays.stream(logger.getHandlers()).forEach(handler -> handler.setLevel(level));
        logger.setLevel(level);

        NodesDescription nodesDescription = new NodesDescription(new String[]{
                "localhost:8091",
                "localhost:8002",
                "localhost:8003",
                "localhost:8004",
                "localhost:8005",
                "localhost:8006",
                "localhost:8007",
                "localhost:8008",
        });

        PCJ.deploy(ReduceTest.class, nodesDescription);
    }

    @Override
    public void main() {
        intValue = PCJ.myId() + 1;
        doubleArray[0] = 1.0 / PCJ.threadCount();
        PCJ.barrier();
        if (PCJ.myId() == 0) {
            int intValueReduced = PCJ.reduce(Integer::sum, Communicable.intValue);
            System.out.println(intValueReduced);

            double doubleArrayReduced = PCJ.reduce(Double::sum, Communicable.doubleArray, 0);
            System.out.println(doubleArrayReduced);
        }
    }
}
