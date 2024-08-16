
import java.util.Scanner;
public class Test2 {
	public static void main(String[] args) {
        
        int l= 12;
        double j= l*4;
        double result1=0,result2=0,result3=0,result4=0;
		for(int i=0;i<100000000;i++){	
            result1 = i * Math.sin(j);
            result2 = i * Math.cos(j);
            result3 = i * Math.sqrt(j);
            result4 = i * Math.exp(j);
            
		}
        System.out.println("sin(j) = " + result1);
        System.out.println("cos(j) = " + result2);
        System.out.println("sqrt(j) = " + result3);
        System.out.println("exp(j) = " + result4);
        System.out.println();
	}
}

class Node{
	Node n;
	int i;
}

