import java.util.*;
import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.jimple.toolkits.annotation.logic.LoopFinder;
import soot.jimple.Stmt;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JNewExpr;
import soot.toolkits.graph.*;
import soot.toolkits.scalar.SimpleLiveLocals;
import soot.toolkits.scalar.LiveLocals;


public class AnalysisTransformer extends SceneTransformer {
    static CallGraph cg;
    
    @Override
    protected void internalTransform(String arg0, Map<String, String> arg1) {
        Set<SootMethod> methods = new HashSet <>();
        cg = Scene.v().getCallGraph();
        // Get the main method
        SootMethod mainMethod = Scene.v().getMainMethod();
        // Get the list of methods reachable from the main method
        // Note: This can be done bottom up manner as well. Might be easier to model.
        getlistofMethods(mainMethod, methods);

        for (SootMethod m : methods) {
            processCFG(m);
        }
    }

    protected static void loopInvariantCheck(List<Stmt> stmtsToRemove, Stmt u, Loop l, boolean f){
        //if check passed
        Value lhs,rhs;
        if(u instanceof JAssignStmt){
            lhs = ((AssignStmt)u).getLeftOp();
            rhs = ((AssignStmt)u).getRightOp();
            if(rhs instanceof JNewExpr || f){
                stmtsToRemove.add(u);
                int i=0;
                for(Stmt s: l.getLoopStatements()){
                    if(!f){
                        if(s == u) stmtsToRemove.add(l.getLoopStatements().get(i+1));
                    }
                    if(s instanceof JAssignStmt){
                        System.out.println("rhs is "+((AssignStmt)s).getRightOp().toString()+" and lhs is "+lhs);
                        
                        if(((AssignStmt)s).getRightOp().toString().equals(lhs.toString())){
                            loopInvariantCheck(stmtsToRemove, s, l, true);
                        }
                    }
                    i++;
                }
            }
            if(rhs instanceof JInvokeStmt){
                // System.out.println("rhs is " + rhs);
            }
        }

    }

    protected static void processCFG(SootMethod method) {
        if(method.toString().contains("init")  ) { return; }
        Body body = method.getActiveBody();
        // Get the callgraph 
        UnitGraph cfg = new BriefUnitGraph(body);
        // Get live local using Soot's exiting analysis
        LiveLocals liveLocals = new SimpleLiveLocals(cfg);
        // Units for the body
        LoopFinder loopFinder = new LoopFinder();
        loopFinder.transform(body);

        // Get the loops identified
        ArrayList<Stmt> stmtsToRemove = null;
        Stmt head = null;
        PatchingChain<Unit> units = body.getUnits();
        ArrayList<Value> rhsList = null;
        ArrayList<Value> lhsList = null;
        for (Loop loop : loopFinder.getLoops(body)) {
            // Print loop information
            rhsList = new ArrayList<>();
            lhsList = new ArrayList<>();
            for(Stmt s: loop.getLoopStatements()){
                if(s instanceof JAssignStmt){
                    rhsList.add(((AssignStmt)s).getRightOp());
                    lhsList.add(((AssignStmt)s).getLeftOp());
                }
            }
            head = loop.getHead();
            stmtsToRemove = new ArrayList<>();   
            int i=0;
            for (Stmt unit : loop.getLoopStatements()) {
                loopInvariantCheck(stmtsToRemove, unit, loop, false);
                System.out.println("Statement inside loop: " + unit.toString());
                i++;
            }

            for(Stmt s: stmtsToRemove){
                units.remove(s);
                units.insertAfter(s, units.getPredOf(head));
            }
        
        }
        // PatchingChain<Unit> units = body.getUnits();
        System.out.println("\n----- " + body.getMethod().getName() + "-----");
        for (Unit u : units) {
            System.out.println("Unit: " + u);
            // List<Local> before = liveLocals.getLiveLocalsBefore(u);
            // List<Local> after = liveLocals.getLiveLocalsAfter(u);
            // System.out.println("Live locals before: " + before);
            // System.out.println("Live locals after: " + after);
            // System.out.println();

        }
    }

    private static void getlistofMethods(SootMethod method, Set<SootMethod> reachableMethods) {
        // Avoid revisiting methods
        if (reachableMethods.contains(method)) {
            return;
        }
        // Add the method to the reachable set
        reachableMethods.add(method);

        // Iterate over the edges originating from this method
        Iterator<Edge> edges = Scene.v().getCallGraph().edgesOutOf(method);
        while (edges.hasNext()) {
            Edge edge = edges.next();
            SootMethod targetMethod = edge.tgt();
            // Recursively explore callee methods
            if (!targetMethod.isJavaLibraryMethod()) {
                getlistofMethods(targetMethod, reachableMethods);
            }
        }
    }
}
