import java.util.Scanner;
public class Test3 {
	public static void main(String[] args) {

        int l= 12;
        double j= l*4;
        double result1=0,result2=0,result3=0,result4=0;
		for(int i=0;i<100000000;i++){	
            result1 = i * Math.sin(j);

            //rhs maths function argument which change every time
            result2 = i * Math.cos(i);

            //updated code with the 
            result3 = i * Math.sqrt(result2);
            result4 = i * Math.pow(2,j);        
		}
		// System.out.println("g.i is "+g.i);
        System.out.println("sin(i) = " + result1);
        System.out.println("cos(i) = " + result2);
        System.out.println("sqrt(i) = " + result3);
        System.out.println("exp(i) = " + result4);
        System.out.println();
	}
}




