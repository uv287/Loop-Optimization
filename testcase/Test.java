import java.util.Scanner;

public class Test {
	
	public static void main(String[] args) {
		
		Node g = null;
		for(int i=0;i<100000000;i++){
			g = new Node();
			g.i = 5;	
		}
		System.out.println("g.i is "+g.i);
	}
}

class Node{
	Node n;
	int i;
}
