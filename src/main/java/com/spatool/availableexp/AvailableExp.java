package com.spatool.availableexp;

import soot.*;
import soot.jimple.JimpleBody;
import soot.jimple.infoflow.data.SootMethodAndClass;
import soot.jimple.internal.JIfStmt;
import soot.options.Options;
import soot.toolkits.graph.ClassicCompleteUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.jimple.AssignStmt;
import soot.jimple.BinopExpr;

import java.io.*;
import java.util.*;

import com.spatool.utils.ArgParse;

public class AvailableExp {

    public static String necessaryArgs[] = {"cp", "c", "m"};

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

        /* meet another fact into this fact, return true if changed */
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
        public AvailableExpFact getOutFact(Unit u) {
            AvailableExpFact fact = unitToFact.get(u);
            if (fact == null) {
                fact = new AvailableExpFact();
                unitToFact.put(u, fact);
            }
            return fact;
        }
    }

    private static class AvailableExpAnalysis {
        private AvailableExpResult result;
        private SootMethod method;
        private JimpleBody body;
        private UnitGraph controlFlowGraph;

        private class WorkList {
            private Queue<Unit> workList;

            public WorkList() {
                workList = new LinkedList<Unit>();
            }

            public void add(Unit u) {
                workList.add(u);
            }

            public Unit remove() {
                return workList.remove();
            }

            public boolean isEmpty() {
                return workList.isEmpty();
            }
        }

        private WorkList workList;

        public AvailableExpAnalysis(SootMethod method) {
            result = new AvailableExpResult();
            this.method = method;
            this.body = (JimpleBody) method.retrieveActiveBody();
            this.controlFlowGraph = new ClassicCompleteUnitGraph(body);
        }

        public AvailableExpResult getResult() {
            return result;
        }

        public AvailableExpFact getInFact(Unit target) {
            AvailableExpFact inFact = new AvailableExpFact();
            boolean firstPred = true;
            for (Unit v : controlFlowGraph.getPredsOf(target)) {
                if (firstPred) {
                    inFact = result.getOutFact(v).clone();
                    firstPred = false;
                } else {
                    inFact.join(result.getOutFact(v));
                }
            }
            return inFact;
        }

        public void doAnalysis() {
            // Initialize the worklist
            workList = new WorkList();
            // Initialize the result
            for (Unit u : body.getUnits()) {
                result.unitToFact.put(u, new AvailableExpFact());
            }
            // Add all units to the worklist
            for (Unit u : body.getUnits()) {
                workList.add(u);
            }
            // Do the analysis
            while (!workList.isEmpty()) {
                Unit u = workList.remove();
                // Get the in fact of the unit
                AvailableExpFact inFact = getInFact(u);
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
                AvailableExpFact outFact = result.getOutFact(u);
                if (outFact.meetInto(inFact)) {
                    // if the out fact changed, add all successors to the worklist
                    for (Unit v : controlFlowGraph.getSuccsOf(u)) {
                        workList.add(v);
                    }
                }
            }
        }

        public void reportResult() {
            System.out.println("------------------------");
            System.out.println("Available Expressions Analysis Result for " + method.getSignature() + ":");
            JimpleBody body = (JimpleBody) method.retrieveActiveBody();
            UnitGraph ug = new ClassicCompleteUnitGraph(body);
            for (Unit u : body.getUnits()) {
                System.out.println("line " + u.getJavaSourceStartLineNumber() + ": " + u.toString());
                // System.out.println("(" + unitIdx + ") " + u.toString() + " at line " + u.getJavaSourceStartLineNumber());
                // also print the predecessors of the unit
                System.out.println("    Preds: " + ug.getPredsOf(u));
                System.out.println("    succs: " + ug.getSuccsOf(u));
                // print available exps
                System.out.println("    Available Expressions: " + result.getOutFact(u));
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
                        AvailableExpFact inFact = getInFact(u);
                        if (inFact.contains(binop)) {
                            System.out.println("line " + u.getJavaSourceStartLineNumber() + ": " + u.toString());
                        }
                    }
                }
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
        if (!ArgParse.check(argMap, necessaryArgs)) {
            return;
        }

        setupSoot();

        // Retrieve printFizzBuzz's body
        SootClass mainClass = Scene.v().getSootClass(argMap.get("c"));
        SootMethod sm = mainClass.getMethodByName(argMap.get("m"));
        JimpleBody body = (JimpleBody) sm.retrieveActiveBody();

        // Print some information about printFizzBuzz
        System.out.println("Method Signature: " + sm.getSignature());
        System.out.println("--------------");
        System.out.println("Argument(s):");
        for (Local l : body.getParameterLocals()) {
            System.out.println(l.getName() + " : " + l.getType());
        }
        System.out.println("--------------");
        System.out.println("This: " + body.getThisLocal());
        System.out.println("--------------");
        System.out.println("Units:");
        int c = 1;
        UnitGraph ug = new ClassicCompleteUnitGraph(body);
        for (Unit u : body.getUnits()) {
            System.out.println("(" + c + ") " + u.toString());
            // also print the predecessors of the unit
            System.out.println("    Preds: " + ug.getPredsOf(u));
            // if the unit is an assignment statement, print the left and right operands
            if (u instanceof AssignStmt) {
                AssignStmt assignStmt = (AssignStmt) u;
                System.out.println("    Assigning to: " + assignStmt.getLeftOp());
                Value rhs = assignStmt.getRightOp();
                System.out.println("    Evaluating: " + rhs);
                // if the right-hand side is a binary operation, print the operands
                if (rhs instanceof BinopExpr) {
                    BinopExpr binop = (BinopExpr) rhs;
                    System.out.println("    Operand 1: " + binop.getOp1());
                    System.out.println("    Operand 2: " + binop.getOp2());
                }
            }
            c++;
        }
        System.out.println("--------------");

        // Print statements that have branch conditions
        System.out.println("Branch Statements:");
        for (Unit u : body.getUnits()) {
            if (u instanceof JIfStmt)
                System.out.println(u.toString());
        }

        AvailableExpAnalysis analysis = new AvailableExpAnalysis(sm);
        analysis.doAnalysis();
        analysis.reportResult();
        analysis.reportWarnings();
    }
}
