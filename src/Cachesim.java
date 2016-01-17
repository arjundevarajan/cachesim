import java.io.*;
import java.util.*;

public class Cachesim {

	//Initialize various global variables
	public static int cacheSize = 0;
	public static int associativity = 0;
	public static int blockSize = 0;
	public static int numBlocks = 0;
	public static int numSets = 0;
	public static int tagSize = 0;
	public static int indexSize = 0;
	public static int blockOffsetSize = 0;

	//24-bit sized addresses
	public static int addressSize = 24;

	//Initialize the cache HashMap and the main memory array
	static HashMap<Integer,Set> cache = new HashMap<Integer,Set>();
	static String[] mainMemory = new String[(int) Math.pow(2, 24)];

	//Pass arguments and text file into variables
	public static void main(String[] args) throws IOException{
		BufferedReader buffer = new BufferedReader(new FileReader(args[0]));
		//Convert the cache size to bytes (from KB)
		cacheSize = 1024*Integer.parseInt(args[1]);
		associativity = Integer.parseInt(args[2]);
		blockSize = Integer.parseInt(args[3]);
		numBlocks = cacheSize/blockSize;
		numSets = numBlocks/associativity;
		//Convert all the sizes into the number of bits each one takes up in the address
		indexSize = (int) (Math.log(numSets)/Math.log(2));
		blockOffsetSize = (int) (Math.log(blockSize)/Math.log(2));
		tagSize = addressSize-indexSize-blockOffsetSize;
		createCache();
		breakDown(buffer);
	}

	//Create the cache itself
	public static void createCache(){
		Cachesim cachesim = new Cachesim();
		Arrays.fill(mainMemory, "00");
		for (int counter=0; counter<numSets; counter++){
			Set set = cachesim.new Set();
			String tagInit = "";
			for (int count=0; count< tagSize; count++){
				tagInit += "0"; 
			}
			int tagInitNum = Integer.parseInt(tagInit);
			for (int cou=0; cou<associativity; cou++){
				String dataInit = "";
				tagInit = decimalToBinary(""+tagInitNum);
				while(tagInit.length()<tagSize){
					tagInit = "0"+tagInit;
				}
				tagInitNum++;
				for (int c=0; c<blockSize; c++){
					dataInit += "00";
				}
				Block block = cachesim.new Block(tagInit,0,0,dataInit);
				set.setOfBlocks[cou] = block;
			}
			cache.put(counter, set);
		}
	}

	//Create the class of the block
	public class Block {
		int validBit, dirtyBit;
		String tag, data;
		public Block(String t, int vb, int db, String d){
			tag = t;
			validBit = vb;
			dirtyBit = db;
			data = d;
		}
	}

	//Create the class of a set, which has a series of blocks (depending on the associativity)
	public class Set {
		Block[] setOfBlocks = new Block[associativity];
		Queue<Block> LRU = new LinkedList<Block>();
	}

	//Break down input txt file into various variables
	public static void breakDown(BufferedReader buf) throws IOException{
		String line;
		while((line = buf.readLine())!=null){
			int space1 = line.indexOf(" ");
			String operation = line.substring(0, space1);
			line = line.substring(space1+1);
			int space2 = line.indexOf(" ");
			String address = line.substring(0, space2);
			line = line.substring(space2+1);
			int accessBytes = 0;
			String storeWord = null;
			if (operation.equals("store")){
				int space3 = line.indexOf(" ");
				accessBytes = Integer.parseInt(line.substring(0,space3));
				storeWord = line.substring(space3+1);
			}
			else{
				accessBytes = Integer.parseInt(line);
			}
			String result = requestMemory(operation,address,accessBytes,storeWord);
			printStuff(operation,address,result);
		}
		buf.close();
	}

	//Request memory from the cache
	public static String requestMemory(String op, String add, int bytes, String sw){
		String missHit = null;

		//Convert hex address to binary
		add = hexToBinary(add);

		//If binary address is not 24 bits, zero-extend the address
		while(add.length()<24){
			add = "0"+add;
		}

		//Separate out the block offset, the index, and the tag from the address in binary
		int blockOffset = 0;
		int index = 0;
		String tag = "0";
		if (blockOffsetSize!=0){
			//Convert blockOffset to decimal
			blockOffset = Integer.parseInt(add.substring(add.length()-blockOffsetSize),2);	
			if (indexSize!=0){
				index = Integer.parseInt(add.substring(add.length()-1-blockOffsetSize-indexSize, add.length()-1-blockOffsetSize));
			}	
			tag = add.substring(0, add.length()-1-blockOffsetSize-indexSize);
		}

		//Depending on the operation, either load something or store something
		if (op.equals("load")){
			missHit = readType(op,add,bytes,blockOffset,index,tag);
		}
		if (op.equals("store")){
			missHit = writeType(op,add,bytes,sw,blockOffset,index,tag);
		}
		return missHit;
	}

	//Print final values
	public static void printStuff(String op, String add, String result){
		System.out.print(op+" "+add+" "+result);
		System.out.println();
	}

	//For load instructions
	public static String readType(String op, String add, int bytes, int blockOffset, int index, String tag){	
		//Loop through the associativity of each set and check to see if tags are equal and if the valid bit = 1
		for(int i=0; i<associativity; i++){
			if (((cache.get(index).setOfBlocks[i].tag).equals(tag))&&((cache.get(index).setOfBlocks[i].validBit)==1)){
				//Add the block to the LRU if it's a hit
				cache.get(index).LRU.add(cache.get(index).setOfBlocks[i]);
//				System.out.println(cache.get(index).setOfBlocks[i].data);
//				System.out.println(blockOffset);
				return "hit"+" "+cache.get(index).setOfBlocks[i].data.substring(blockOffset, 2*bytes+blockOffset);
			}
		}

		//Locate the block to use if it's a miss
		Block block = locateBlock(index);

		//Check if the block's data is dirty
		dirtyCheck(add,block);

		int missNum = 0;

		return "miss"+" "+missNum;
	}

	//For store instructions
	public static String writeType(String op, String add, int bytes, String sw, int blockOffset, int index, String tag){
		//Loop through the associativity of each set and check to see if the tags are equal and if the valid bit = 1
		for(int i=0; i<associativity; i++){
			if (((cache.get(index).setOfBlocks[i].tag).equals(tag))&&((cache.get(index).setOfBlocks[i].validBit)==1)){
				cache.get(index).setOfBlocks[i].data = sw;
				cache.get(index).setOfBlocks[i].dirtyBit = 1;
				return "hit";
			}
		}

		//Locate the block to use if it's a miss
		Block block = locateBlock(index);
//		System.out.println(block.data+" "+block.dirtyBit+" "+block.tag+" "+block.validBit);

		//Check if the block's data is dirty
		dirtyCheck(add,block);

		return "miss";
	}

	//If it's a miss, find the next available block
	public static Block locateBlock(int index){
		//If it's not a hit, then locate a block to use by either taking the first nonvalid bit or by taking the LRU
		Block block = null;
		for (int j=0; j<associativity; j++){
			if(cache.get(index).setOfBlocks[j].validBit==0){
				block = cache.get(index).setOfBlocks[j];
			}
		}
		if (block==null){
			block = cache.get(index).LRU.poll();
		}

		//Add the block to the LRU if it's being used
		cache.get(index).LRU.add(block);

		return block;
	}

	public static void dirtyCheck(String add, Block block){
		//		if (block.dirtyBit==1){
		//			add = hexToDecimal(binaryToHex(add));
		//			mainMemory[add] = block.data
		//			return;
		//		}
		//		else{
		//			return;
		//		}
	}

	//Convert hex to binary (0x___)->(________________)
	public static String hexToBinary(String str){
		int binary = Integer.parseInt(str.substring(2),16);
		return Integer.toBinaryString(binary);
	}

	//Convert binary back to hex (no "0x")
	public static String binaryToHex(String str){
		int decimal = Integer.parseInt(str,2);
		return Integer.toString(decimal,16);
	}

	//Convert hex to decimal
	public static String hexToDecimal(String str){
		return ""+Integer.parseInt(str,16);
	}

	//Convert decimal to hex (no "0x")
	public static String decimalToHex(String str){
		return Integer.toHexString(Integer.parseInt(str));
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
