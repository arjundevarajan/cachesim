
public class varPlus {
	public static void main(String[] args){
		int tagSize = 5;
		int associativity = 4;
		int numSets = 3;
		for (int num=0; num<numSets; num++){
			String tagInit = "";
			for (int c=0; c< tagSize; c++){
				tagInit += "0"; 
			}
			int tagInitNum = Integer.parseInt(tagInit);
			for (int count=0; count<associativity; count++){
				tagInit = decimalToBinary(""+tagInitNum);
				while(tagInit.length()<tagSize){
					tagInit = "0"+tagInit;
				}
				tagInitNum++;
				System.out.println(tagInit);
			}
		}
	}
	//Convert binary to decimal
	public static String binaryToDecimal(String s){
		return Integer.toString(Integer.parseInt(s, 2));
	}

	//Convert decimal to binary
	public static String decimalToBinary(String s){
		return Integer.toBinaryString(Integer.parseInt(s));
	}

}
