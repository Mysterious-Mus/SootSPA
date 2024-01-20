package com.spatool.availableexp;

import soot.*;
import soot.jimple.JimpleBody;
import soot.options.Options;
import soot.toolkits.graph.ClassicCompleteUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.jimple.AssignStmt;
import soot.jimple.BinopExpr;

import java.io.File;
import java.util.*;

import com.spatool.utils.ArgParse;
import com.spatool.utils.SourceCode;
import com.spatool.utils.WorkList;

public class AvailableExp {

    public static String necessaryArgs[] = {"cp", "c", "m"};
    public static String optionalArgs[] = {"may_iter", "enhanced_may_iter", "report_performance", "use_iterative"};

    private static class AvailableExpFact implements Iterable<BinopExpr>{
        private Set<BinopExpr> availableExps;

        @Override
        public Iterator<BinopExpr> iterator() {
            return availableExps.iterator();
        }

        public AvailableExpFact() {
            availableExps = new HashSet<BinopExpr>();
        }

        public boolean contains(BinopExpr binop) {
            for (BinopExpr myop : availableExps) {
                if (myop.equivTo(binop)) {
                    return true;
                }
            }
            return false;
        }

        /* set the receiving obj to the join of it and another fact */
        public void join(AvailableExpFact another) {
            Set<BinopExpr> newAvailableExps = new HashSet<BinopExpr>();
            for (BinopExpr binop : availableExps) {
                if (another.contains(binop)) {
                    newAvailableExps.add(binop);
                }
            }
            availableExps = newAvailableExps;
        }

        public void add(BinopExpr binop) {
            availableExps.add(binop);
        }

        /**
         *  meet another fact into this fact, return true if changed */
        public boolean meetInto(AvailableExpFact another) {
            boolean changed = false;
            for (BinopExpr binop : another) {
                if (!contains(binop)) {
                    add(binop);
                    changed = true;
                }
            }
            return changed;
        }

        @Override
        public String toString() {
            String result = "";
            for (BinopExpr binop : availableExps) {
                result += binop.toString() + ", ";
            }
            return result;
        }

        public AvailableExpFact clone() {
            AvailableExpFact newFact = new AvailableExpFact();
            for (BinopExpr binop : availableExps) {
                newFact.add(binop);
            }
            return newFact;
        }
    }

    /* the class to store out facts of the units */
    private static class AvailableExpResult {
        private Map<Unit, AvailableExpFact> unitToFact;

        public AvailableExpResult() {
            unitToFact = new HashMap<Unit, AvailableExpFact>();
        }

        /* get the out fact of the given unit, report an empty one if the unit is not recorded yet */
        public AvailableExpFact getFact(Unit u) {
            AvailableExpFact fact = unitToFact.get(u);
            if (fact == null) {
                fact = new AvailableExpFact();
                unitToFact.put(u, fact);
            }
            return fact;
        }

        public void put(Unit u, AvailableExpFact fact) {
            unitToFact.put(u, fact);
        }
    }

    private static class AvailableExpAnalysis {
        private AvailableExpResult outFacts;
        private SootMethod method;
        private JimpleBody body;
        private UnitGraph controlFlowGraph;
        private Map<String, String> args;

        private WorkList<Unit> workList;

        public AvailableExpAnalysis(SootMethod method, Map<String, String> args) {
            this.args = args;
            outFacts = new AvailableExpResult();
            this.method = method;
            this.body = (JimpleBody) method.retrieveActiveBody();
            this.controlFlowGraph = new ClassicCompleteUnitGraph(body);
            this.workList = new WorkList<Unit>(args);
            // Initialize the result
            for (Unit u : body.getUnits()) {
                outFacts.put(u, new AvailableExpFact());
            }
            // Add all units to the worklist
            for (Unit u : body.getUnits()) {
                workList.add(u);
            }
        }

        public AvailableExpResult getOutFacts() {
            return outFacts;
        }

        public AvailableExpFact getInFactJoin(Unit target) {
            AvailableExpFact inFact = new AvailableExpFact();
            boolean firstPred = true;
            for (Unit v : controlFlowGraph.getPredsOf(target)) {
                if (firstPred) {
                    inFact = outFacts.getFact(v).clone();
                    firstPred = false;
                } else {
                    inFact.join(outFacts.getFact(v));
                }
            }
            return inFact;
        }

        public AvailableExpFact getInFactMeet(Unit target) {
            AvailableExpFact inFact = new AvailableExpFact();
            for (Unit v : controlFlowGraph.getPredsOf(target)) {
                inFact.meetInto(outFacts.getFact(v));
            }
            return inFact;
        }

        /**
         * propagate the unit, return true if the out fact changed
         * @param u
         * @return
         */
        public boolean propagateUnit(Unit u) {
            // Get the in fact of the unit
            AvailableExpFact inFact = getInFactJoin(u);
            // in fact refers to all the new available expressions ready to add to the out fact
            // Get the out fact of the unit
            // first kill changed expressions
            if (u instanceof AssignStmt) {
                AssignStmt assignStmt = (AssignStmt) u;
                // the variable assigned to
                Value lhs = assignStmt.getLeftOp();
                // see if any expression uses the variable in inFact, kill it if so
                Iterator<BinopExpr> iter = inFact.iterator();
                while (iter.hasNext()) {
                    BinopExpr binop = iter.next();
                    if (binop.getOp1().equivTo(lhs) || binop.getOp2().equivTo(lhs)) {
                        iter.remove();
                    }
                }
            }
            // since IR won't assign to a variable used, we can handle generated expressions after kill
            if (u instanceof AssignStmt) {
                AssignStmt assignStmt = (AssignStmt) u;
                // the expression generated
                Value rhs = assignStmt.getRightOp();
                if (rhs instanceof BinopExpr) {
                    BinopExpr binop = (BinopExpr) rhs;
                    inFact.add(binop);
                }
            }
            // merge the in fact into the out fact
            AvailableExpFact outFact = outFacts.getFact(u);
            return outFact.meetInto(inFact);
        }

        /**
         * finish all the remaining worklist
         * can be called both from outside and from may iter
         */
        public void doAnalysis() {
            if (args.get("use_iterative") == null) {
                while (!workList.isEmpty()) {
                    Unit u = workList.remove();
                    if (propagateUnit(u)) {
                        // if the out fact changed, add all successors to the worklist
                        for (Unit v : controlFlowGraph.getSuccsOf(u)) {
                            workList.add(v);
                        }
                    }
                }
            }
            else {
                if (!workList.isEmpty()) {
                    workList.clear();
                    boolean changed = true;
                    while (changed) {
                        changed = false;
                        for (Unit u : body.getUnits()) {
                            workList.add(u); workList.remove();
                            if (propagateUnit(u)) {
                                changed = true;
                            }
                        }
                    }
                }
            }
        }

        public void doMayIter(int Niter, boolean enhanced) {
            for (int iter = 0; iter < Niter; iter++) {
                System.out.println("---------------------------------------");
                System.out.println("may iter " + iter);
                AvailableExpResult inFacts = new AvailableExpResult();
                // figure out all the in facts
                for (Unit u : body.getUnits()) {
                    AvailableExpFact inFact = getInFactMeet(u);
                    // we don't have to do gen anymore, we only have to kill
                    if (u instanceof AssignStmt) {
                        AssignStmt assignStmt = (AssignStmt) u;
                        // the variable assigned to
                        Value lhs = assignStmt.getLeftOp();
                        // see if any expression uses the variable in inFact, kill it if so
                        Iterator<BinopExpr> iterIn = inFact.iterator();
                        while (iterIn.hasNext()) {
                            BinopExpr binop = iterIn.next();
                            if (binop.getOp1().equivTo(lhs) || binop.getOp2().equivTo(lhs)) {
                                iterIn.remove();
                            }
                        }
                    }
                    inFacts.put(u, inFact);
                }
                // meet in facts into out facts
                for (Unit u : body.getUnits()) {
                    System.out.println("line " + u.getJavaSourceStartLineNumber() + ": " + u.toString());
                    System.out.println("    Meeting in: " + inFacts.getFact(u));
                    AvailableExpFact outFact = outFacts.getFact(u);
                    boolean changed = outFact.meetInto(inFacts.getFact(u));
                    if (changed && enhanced) {
                        // if the out fact changed, add all successors to the worklist
                        for (Unit v : controlFlowGraph.getSuccsOf(u)) {
                            workList.add(v);
                        }
                    }
                }
                if (enhanced) {
                    doAnalysis();
                }
            }
        }

        public void reportResult() {
            System.out.println("------------------------");
            System.out.println("Available Expressions Analysis Result for " + method.getSignature() + ":");
            JimpleBody body = (JimpleBody) method.retrieveActiveBody();
            UnitGraph ug = new ClassicCompleteUnitGraph(body);
            for (Unit u : body.getUnits()) {
                System.out.println("line " + u.getJavaSourceStartLineNumber() + ": " + u);
                // System.out.println("(" + unitIdx + ") " + u.toString() + " at line " + u.getJavaSourceStartLineNumber());
                // also print the predecessors of the unit
                System.out.println("    Preds: " + ug.getPredsOf(u));
                System.out.println("    succs: " + ug.getSuccsOf(u));
                // print available exps
                System.out.println("    Available Expressions: " + outFacts.getFact(u));
            }
        }

        /* report all the redundant computations  */
        public void reportWarnings() {
            System.out.println("------------------------");
            System.out.println("Units you may optimize in " + method.getSignature() + ":");
            JimpleBody body = (JimpleBody) method.retrieveActiveBody();
            UnitGraph ug = new ClassicCompleteUnitGraph(body);
            for (Unit u : body.getUnits()) {
                // if an bin expr used in this unit is available in its in fact
                if (u instanceof AssignStmt) {
                    AssignStmt assignStmt = (AssignStmt) u;
                    Value rhs = assignStmt.getRightOp();
                    if (rhs instanceof BinopExpr) {
                        BinopExpr binop = (BinopExpr) rhs;
                        AvailableExpFact inFact = getInFactJoin(u);
                        if (inFact.contains(binop)) {
                            System.out.println(args.get("cp") + File.separator + args.get("c") + ".java:" + u.getJavaSourceStartLineNumber() + " " +
                                SourceCode.get(args.get("cp"), args.get("c"), u.getJavaSourceStartLineNumber()));
                        }
                    }
                }
            }
        }

        /** report how many times a unit is handled, i.e. how many times is the worklist polled */
        public void reportPerformance() {
            System.out.println("------------------------");
            System.out.println("Performance Report for " + method.getSignature() + ":");
            System.out.println("    Unit propagation count: " + workList.getPollCount());
        }

        public void main() {
            doAnalysis();
            if (args.get("may_iter") != null) {
                doMayIter(Integer.parseInt(args.get("may_iter")), args.get("enhanced_may_iter") != null);
            }
            reportResult();
            reportWarnings();
            if (args.get("report_performance") != null) {
                reportPerformance();
            }
        }
    }

    public static void setupSoot() {
        G.reset();
        Options.v().set_prepend_classpath(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_soot_classpath(argMap.get("cp"));
        Options.v().set_keep_line_number(true);
        Options.v().set_keep_offset(true);
        SootClass sc = Scene.v().loadClassAndSupport(argMap.get("c"));
        sc.setApplicationClass();
        Scene.v().loadNecessaryClasses();

    }

    private static Map<String, String> argMap;

    public static void main(String[] args) {
        // print args
        System.out.println("args: " + Arrays.toString(args));

        argMap = ArgParse.parse(args);
        if (argMap == null || !ArgParse.check(argMap, necessaryArgs, optionalArgs)) {
            return;
        }

        setupSoot();

        // Retrieve printFizzBuzz's body
        SootClass mainClass = Scene.v().getSootClass(argMap.get("c"));
        SootMethod sootMethod = mainClass.getMethodByName(argMap.get("m"));
        JimpleBody methodBody = (JimpleBody) sootMethod.retrieveActiveBody();

        // Print some information about printFizzBuzz
        System.out.println("Method Signature: " + sootMethod.getSignature());
        System.out.println("--------------");
        System.out.println("Argument(s):");
        for (Local l : methodBody.getParameterLocals()) {
            System.out.println(l.getName() + " : " + l.getType());
        }
        System.out.println("--------------");
        System.out.println("This: " + methodBody.getThisLocal());
        System.out.println("--------------");
        System.out.println("Units:");
        int c = 1;
        UnitGraph ug = new ClassicCompleteUnitGraph(methodBody);
        for (Unit u : methodBody.getUnits()) {
            System.out.println("(" + c + ") " + u.toString());
            // // also print the predecessors of the unit
            // System.out.println("    Preds: " + ug.getPredsOf(u));
            // // if the unit is an assignment statement, print the left and right operands
            // if (u instanceof AssignStmt) {
            //     AssignStmt assignStmt = (AssignStmt) u;
            //     System.out.println("    Assigning to: " + assignStmt.getLeftOp());
            //     Value rhs = assignStmt.getRightOp();
            //     System.out.println("    Evaluating: " + rhs);
            //     // if the right-hand side is a binary operation, print the operands
            //     if (rhs instanceof BinopExpr) {
            //         BinopExpr binop = (BinopExpr) rhs;
            //         System.out.println("    Operand 1: " + binop.getOp1());
            //         System.out.println("    Operand 2: " + binop.getOp2());
            //     }
            // }
            c++;
        }

        AvailableExpAnalysis analysis = new AvailableExpAnalysis(sootMethod, argMap);
        analysis.main();
    }
}
