import java.util.*;
import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.jimple.toolkits.annotation.logic.LoopFinder;
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
        getlistofMethods(mainMethod, methods);

        for (SootMethod m : methods) {
            processCFG(m);
        }
    }

    // protected static void loopInvariantCheck(List<Stmt> stmtsToRemove, Stmt u, Loop l, boolean f){
    //     Value lhs,rhs;
    //     if(u instanceof JAssignStmt){
    //         lhs = ((AssignStmt)u).getLeftOp();
    //         rhs = ((AssignStmt)u).getRightOp();
    //         if(rhs instanceof JNewExpr || f){
    //             stmtsToRemove.add(u);
    //             int i=0;
    //             for(Stmt s: l.getLoopStatements()){
    //                 if(!f){
    //                     if(s == u) stmtsToRemove.add(l.getLoopStatements().get(i+1));
    //                 }
    //                 if(s instanceof JAssignStmt){
    //                     System.out.println("rhs is "+((AssignStmt)s).getRightOp().toString()+" and lhs is "+lhs);
                        
    //                     if(((AssignStmt)s).getRightOp().toString().equals(lhs.toString())){
    //                         loopInvariantCheck(stmtsToRemove, s, l, true);
    //                     }
    //                 }
    //                 i++;
    //             }
    //         }
    //         if(rhs instanceof JInvokeStmt){
    //             // System.out.println("rhs is " + rhs);
    //         }
    //     }

    // }

    protected static void processCFG(SootMethod method) {

        if(method.toString().contains("init")  ) { return; }
        
        Body body = method.getActiveBody();
        // Get the callgraph 
        
        UnitGraph cfg = new BriefUnitGraph(body);
        
        // Get live local using Soot's exiting analysis
        LiveLocals liveLocals = new SimpleLiveLocals(cfg);
        
        // Units body for the loop
        LoopFinder loopFinder = new LoopFinder();
        loopFinder.transform(body);

        PatchingChain<Unit> units = body.getUnits();

        // for (Unit u : body.getUnits()) {

        //     // Check if the statement is an assignment statement
        //     if (u instanceof JAssignStmt) {
        //         JAssignStmt assignStmt = (JAssignStmt) u;

        //         // Get the right-hand side (RHS) expression of the assignment
        //         Value rhs = assignStmt.getRightOp();

        //         // Check if the RHS is not an invocation expression or if it's not a mathematical function
        //         if (!(rhs instanceof InvokeExpr) || !isMathFunction((InvokeExpr) rhs)) {
        //             // Add the left-hand side (LHS) to the list
        //             lhsList.add(assignStmt.getLeftOp());
        //         }
        //     }
        // }

        // System.out.println("Left-hand sides of assignment statements where the RHS is not a mathematical function:");
        // for (Value lhs : lhsList) {
        //     System.out.println(lhs);
        // }


        //loop body
        for (Loop loop : loopFinder.getLoops(body)) {
            
            // last statement
            Unit backBranch = loop.getBackJumpStmt();

            //to store the new statement
            List<AssignStmt> newExpressionStmts = new ArrayList<>();

            //to statement whoes rhs is the invok expression
            List<AssignStmt> MathExpressionStmts = new ArrayList<>();

            ArrayList<Value> lhsList = new ArrayList<>();

            for (Unit u : loop.getLoopStatements()) {
                // Check if the statement is an assignment statement
                if (u instanceof JAssignStmt) {
                    JAssignStmt assignStmt = (JAssignStmt) u;
    
                    // Get the right-hand side (RHS) expression of the assignment
                    Value rhs = assignStmt.getRightOp();
    
                    // Check if the RHS is not an invocation expression or if it's not a mathematical function
                    if (!(rhs instanceof InvokeExpr) || !isMathFunction((InvokeExpr) rhs)) {
                        // Add the left-hand side (LHS) to the list
                        lhsList.add(assignStmt.getLeftOp());
                    }
                }
            }

            System.out.println(lhsList);

            // Iterate through the statements within each loop
            for (Unit u : loop.getLoopStatements()) {
            
                // Check if the statement is an assignment of a new expression (e.g., "$r2 = new Node")
                if (u instanceof AssignStmt) {
                    AssignStmt assignStmt = (AssignStmt) u;
                    Value rightOp = assignStmt.getRightOp();
                    
                    if (rightOp instanceof NewExpr) {
                        // This is a new expression statement inside the loop
                        newExpressionStmts.add(assignStmt);
                    }

                    //identified the math function
                    else if (rightOp instanceof InvokeExpr) {

                        InvokeExpr invokeExpr = (InvokeExpr) rightOp;
                        
                        // Check if the method being invoked is a static method from java.lang.Math
                        SootMethod m = invokeExpr.getMethod();
                        if (m.getDeclaringClass().getName().equals("java.lang.Math")) {

                            MathExpressionStmts.add(assignStmt);

                            // This is an assignment of a mathematical function call from java.lang.Math
                            System.out.println("Assignment of mathematical function call: " + m.getName());
                        }
                    }
                }
            }
            
            // for (AssignStmt stmt : newExpressionStmts) {
            //     System.out.println(stmt);
            // }

            for (AssignStmt stmt : MathExpressionStmts) {
                System.out.println(stmt);
            }
            
            //iterate assignment statements whoes rhs is the maths function
            for (AssignStmt stmt : MathExpressionStmts) {
                Value lhs = (Value) stmt.getLeftOp();
                InvokeExpr rhs = (InvokeExpr) stmt.getRightOp();


                
                //check wether the lhs is updated or not and arguments are updated or not
                if(!(lhsList.contains(lhs)))
                {
                    System.out.println("lhs");

                    boolean j = false;  

                    // for (Value arg : rhs.getArgs()) {
                    //     // argument also not modified
                    //     System.out.println(arg);
                    //     if(lhsList.contains(arg));
                    //     {
                    //         j= true;
                    //     }
                    // }
                    
                    System.out.println(j);

                    if(!j)
                    {
                        boolean removed =units.remove(stmt);

                            if(removed)
                            {
                                System.out.println("sucess");
                            }
                            else{
                                System.out.println("fail");
                            }

                            Unit h = loop.getHead();
                            System.out.println(units.getPredOf(h).toString());
                            units.insertBefore(stmt,units.getPredOf(h));
                    }
                }

            }

            // Iterate through each new expression statement found inside the loop
            for (AssignStmt newExpressionStmt : newExpressionStmts) {
                // Get the variable introduced by the new expression
                Local newExpressionVariable = (Local) newExpressionStmt.getLeftOp();

                // System.out.println("hello1");

                boolean j;

                j=false;

                // Check if the new expression variable is live after the loop
                List<Local> after = liveLocals.getLiveLocalsAfter(backBranch);

                if(after.contains(newExpressionVariable))
                {
                    // The variable is live after the loop
                    j = true;

                    // System.out.println("hello1.5");
                    // System.out.println("hello1.75");

                }


                // System.out.println("hello2");

                

                // If the variable is not live after the loop, move the new expression statement before the loop
                if (!j) {
                    boolean removed =units.remove(newExpressionStmt);

                    if(removed)
                    {
                        System.out.println("sucess");
                    }
                    else{
                        System.out.println("fail");
                    }

                    Unit h = loop.getHead();
                    // System.out.println(units.getPredOf(h).toString());
                    units.insertBefore(newExpressionStmt,units.getPredOf(h));
                }
            }

            // for (Unit u : loop.getLoopStatements()) {
            //     // Print the statement

            //     List<Local> before = liveLocals.getLiveLocalsBefore(u);
            //     List<Local> after = liveLocals.getLiveLocalsAfter(u);
            //     System.out.println(u);
            //     System.out.println("Live locals before: " + before);
            //     System.out.println("Live locals after: " + after);
            //     System.out.println();

            //     if (u.equals(backBranch)) {
            //         System.out.println("Last statement in the loop.");
            //         System.out.println("Live locals before: " + before);
            //     System.out.println("Live locals after: " + after);
            //     }
                
            // }
        }
        // for (Loop loop : loopFinder.getLoops(body)) {
        //     // Print loop information
        //     rhsList = new ArrayList<>();
        //     lhsList = new ArrayList<>();
        //     for(Stmt s: loop.getLoopStatements()){
        //         if(s instanceof JAssignStmt){
        //             rhsList.add(((AssignStmt)s).getRightOp());
        //             lhsList.add(((AssignStmt)s).getLeftOp());
        //         }
        //     }
        //     head = loop.getHead();
        //     stmtsToRemove = new ArrayList<>();   
        //     int i=0;
        //     for (Stmt unit : loop.getLoopStatements()) {
        //         loopInvariantCheck(stmtsToRemove, unit, loop, false);
        //         System.out.println("Statement inside loop: " + unit.toString());
        //         i++;
        //     }

        //     for(Stmt s: stmtsToRemove){
        //         units.remove(s);
        //         units.insertAfter(s, units.getPredOf(head));
        //     }
        
        // }
        // // PatchingChain<Unit> units = body.getUnits();
        // System.out.println("\n----- " + body.getMethod().getName() + "-----");
        // for (Unit u : units) {
        //     System.out.println("Unit: " + u);
        //     // List<Local> before = liveLocals.getLiveLocalsBefore(u);
        //     // List<Local> after = liveLocals.getLiveLocalsAfter(u);
        //     // System.out.println("Live locals before: " + before);
        //     // System.out.println("Live locals after: " + after);
        //     // System.out.println();

        // }
    }

    private static boolean isMathFunction(InvokeExpr invokeExpr) {
        SootMethod method = invokeExpr.getMethod();
        return method.getDeclaringClass().getName().equals("java.lang.Math");
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
