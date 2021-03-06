import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

public class ARC_Algo { 	//TODO better array to image and vice versa ((no loss dapat))
							//TODO tests for 512 and 256 images
							//TODO when to stop extracting
	int[][] rangeTableA = 	{{0,8,16,32,64}, 	//lj; TABLE FOR SMOOTH
		     				{7,15,31,63,255}, 	//uj
		     				{0,0,0,0,0}};		//tj
	int[][] rangeTableB = 	{{0,8,16,32,64}, 	//lj; TABLE FOR EDGY
		     				{7,15,31,63,255}, 	//uj
		     				{0,0,0,0,0}};		//tj
	
	private int[][] imageGrid;							//each element has the equivalent pixel value ((decimal))
	private MessageHelper secretMessage;				//converted text file ((binary))
	private List<Block> blocks;
	int counter = 0;									//temp stopper for extraction
	ARC_Algo(int[][] imageGrid, MessageHelper secretMessage) {	//constructor
		this.imageGrid = imageGrid;
		this.secretMessage = secretMessage;
		rangeTableA[2] = getEmbeddableBits(rangeTableA);
		rangeTableB[2] = getEmbeddableBits(rangeTableB);
	}
	
	public void embedImage() throws IOException {
		boolean isSmooth = imageClassification(imageGrid); 	//Module 1
		int[][] rangeTable = (isSmooth) ? rangeTableA : rangeTableB; 
		embedTableClue(imageGrid[0][0], isSmooth);				//Module 2
		blocks = ImageHelper.pixelDivision(imageGrid);				//Module 3
		for(int i = 1; i < blocks.size(); i++){			//embedding phase
			if(secretMessage.getCurrentBit() <= secretMessage.getFinalBit()){
				embedBlock(blocks.get(i), secretMessage, rangeTable);
				counter++;
			}
		}
		printBlockInfo(blocks, blocks, "BlocksInfoEmbedded");
		updateGrid(imageGrid, blocks);
		ImageHelper.createStegoImage(imageGrid, "ARCStegoImage");
	}
	
	public void extractMessage(BufferedImage stegoImage) throws IOException {
		int[][] embeddedStegoGrid = new int[stegoImage.getHeight()][stegoImage.getWidth()];
		embeddedStegoGrid = ImageHelper.getImagePixelValues(stegoImage, embeddedStegoGrid);
		int[][] rangeTable = (isTableA(embeddedStegoGrid[0][0])) ? rangeTableA : rangeTableB;
		List<Block> embeddedBlocks = ImageHelper.pixelDivision(embeddedStegoGrid);
		String embeddedSecretMessage = "";
		for(int i = 1; i < counter; i++) {
			embeddedSecretMessage += extractBlock(embeddedBlocks.get(i), rangeTable);
		}
		printBlockInfo(embeddedBlocks, embeddedBlocks, "BlocksInfoExtracted");
		//testing(blocks, embeddedBlocks);
		MessageHelper.writeMessage(embeddedSecretMessage);
		System.out.println(MessageHelper.binaryToASCII("ExtractedMessageBinary.txt"));
	}
	
	
	public static boolean imageClassification(int[][] stegoGrid) { //TODO
		
		
		return true;
	}
	
	public static void embedTableClue(int pixel, boolean isSmooth) {
		if(isSmooth) {
			//embed on pix; 0
		} else {
			//embed on pix
		}
	}
	
	public static boolean isTableA(int firstPixel){
		int firstPixelLSB = firstPixel & ((1 << 1) - 1);
		if(firstPixelLSB == 0) {
			return true;
		} else {
			return false;
		}
	}
	public static void updateGrid(int[][] stegoGrid, List<Block> blocks) {
		int counter = 0;
		for(int i = 0; i < stegoGrid.length; i++) { //replace each block's value to image
			for(int j = 0; j < stegoGrid[0].length - 2; j += 3){
				stegoGrid[j][i] = blocks.get(counter).getLeftPixel();
				stegoGrid[j+1][i] = blocks.get(counter).getBasePixel();
				stegoGrid[j+2][i] = blocks.get(counter).getRightPixel();
				counter++;
			}
		}
	}
	
	public static void embedBlock(Block block, MessageHelper secretMessage, int[][] rangeTable) {
		int basePixel3LSB = (byte) (block.getBasePixel() & ((1 << 3) - 1)); 
		String toEmbed = secretMessage.peekSecretBits(3);
		int secretMessageLSB = Integer.parseInt(secretMessage.peekSecretBits(3), 2);
		//LSB
		int embeddedBasePixel = replaceLeastSignificantBit
				(block.getBasePixel(), 2, Integer.parseInt(secretMessage.getSecretBits(3), 2)); 
		//OPAP
		block.setBasePixel(opapPixel(basePixel3LSB, secretMessageLSB, embeddedBasePixel));
		
		//DETERMINE RANGE
		
		block.setLeftRange(locateTableRange(Math.abs(block.getBasePixel() - block.getLeftPixel()), rangeTable));
		block.setRightRange(locateTableRange(Math.abs(block.getBasePixel() - block.getRightPixel()), rangeTable));
		//PVD ((EMBEDDING OF SECRET MESSAGE))
		toEmbed += secretMessage.peekSecretBits(rangeTable[2][block.getRangeIndex("left")]);
		int embeddedLeftPixel = embedPixelPVD("left", block, secretMessage, rangeTable);
		toEmbed += secretMessage.peekSecretBits(rangeTable[2][block.getRangeIndex("right")]);
		int embeddedRightPixel = embedPixelPVD("right", block, secretMessage, rangeTable);
		
		block.setLeftPixel(embeddedLeftPixel);
		block.setRightPixel(embeddedRightPixel);
		block.setEmbedded(toEmbed);
	}
	
	public static int embedPixelPVD(String pixel, Block block, MessageHelper secretMessage, int[][] rangeTable) {
		int embeddableBits = rangeTable[2][block.getRangeIndex(pixel)];
		int secretM = Integer.parseInt(secretMessage.getSecretBits(embeddableBits), 2);
		int newDiff = rangeTable[0][block.getRangeIndex(pixel)] + secretM;
		
		int embeddedPixelTemp1 = block.getBasePixel() + newDiff;
		int embeddedPixelTemp2 = block.getBasePixel() - newDiff;
		
		if(pixel.equals("left")) {
			if(Math.abs(block.getLeftPixel() - embeddedPixelTemp1) < Math.abs(block.getLeftPixel() - embeddedPixelTemp2)) {
				return embeddedPixelTemp1;
			} else {
				return embeddedPixelTemp2;
			}
		} else { 
			if(Math.abs(block.getRightPixel() - embeddedPixelTemp1) < Math.abs(block.getRightPixel() - embeddedPixelTemp2)) {
				return embeddedPixelTemp1;
			} else {
				return embeddedPixelTemp2;
			}
		}
		
		
	}
	public static int locateTableRange(int diff, int[][] rangeTable) {
		int index = 0;
		for(int i = 0; i < rangeTable[0].length; i++){
			if(diff >= rangeTable[0][i] && diff <= rangeTable[1][i]){
				index = i;
				break;
			}
		}
		return index;
	}
	
	public static int opapPixel(int basePixel3LSB, int secretMessageLSB, int embeddedBasePixel) {
		int baseDifference = basePixel3LSB - secretMessageLSB;
		
		if(baseDifference > Math.pow(2, 2) && (embeddedBasePixel + Math.pow(2, 3) >= 0) && (embeddedBasePixel + Math.pow(2, 3) <= 255)) {
			embeddedBasePixel += Math.pow(2, 3); 
		} else if(baseDifference < (-1 * Math.pow(2,2)) && (embeddedBasePixel - Math.pow(2, 3) <= 255) && (embeddedBasePixel - Math.pow(2, 3) >= 0)) {
			embeddedBasePixel -= Math.pow(2, 3);
		}
		
		return embeddedBasePixel;
	}
	
	public static int replaceLeastSignificantBit(int pixel, int bitsToReplace, int toEmbed) {
		//make the bits 0
		int modifiedPixel = pixel;
		for(int i = 0; i <= bitsToReplace; i++) {
			modifiedPixel = (byte) (modifiedPixel & ~(1 << i)) & 0xff; 			//make 3 LSB 0
		}
		modifiedPixel = ((byte) modifiedPixel | (byte)toEmbed)& 0xff ; 
		return modifiedPixel;
	}
	
	public static int getLeastSignificantBit(int embeddedPixel, int bitsToGet) {
		return (byte) (embeddedPixel & ((1 << bitsToGet) - 1));
	}
	public static int[] getEmbeddableBits(int[][] rangeTable) {
		int[] embeddableSecretBits = new int[rangeTable[0].length];
		for(int i = 0; i < rangeTable[0].length; i++) {
			embeddableSecretBits[i] = (i <= 3) ? 
					(int)Math.round((Math.log(rangeTable[1][i]-rangeTable[0][i]) / Math.log(2))): 
					(int)Math.round((Math.log(rangeTable[0][i]) / Math.log(2)));
			//System.out.print(embeddableSecretBits[i] + " ");
		}
		return embeddableSecretBits;
	}
	
	public static int extractPVD(String dir, int diff, int[][] rangeTable, Block block) {
		if(dir.equals("left")) {
			return Math.abs(diff - rangeTable[0][block.getLeftRange()]);
		} else {
			return Math.abs(diff - rangeTable[0][block.getRightRange()]);
		}
	}
	
	public static String extractBlock(Block block, int[][] rangeTable) {
		//LSB Extraction
		int basePixelGet3LSB = (byte) (block.getBasePixel() & ((1 << 3) - 1));
		
		int diffL = Math.abs(block.getLeftPixel() - block.getBasePixel());
		int diffR = Math.abs(block.getRightPixel() - block.getBasePixel());
		
		block.setLeftRange(locateTableRange(diffL, rangeTable));
		block.setRightRange(locateTableRange(diffR, rangeTable));
		
		int secretMessageLeft = extractPVD("left", diffL, rangeTable, block);
		int secretMessageRight = extractPVD("right", diffR, rangeTable, block);
		
		String basePixelMessage = foo(3, Integer.toBinaryString(basePixelGet3LSB));
		String leftPixelMessage = foo(rangeTable[2][block.getLeftRange()], Integer.toBinaryString(secretMessageLeft));
		String rightPixelMessage = foo(rangeTable[2][block.getRightRange()], Integer.toBinaryString(secretMessageRight));
		
		//System.out.println(leftPixelMessage+" "+rangeTable[2][block.getLeftRange()]);
		//System.out.println(rightPixelMessage+" "+rangeTable[2][block.getRightRange()]);
		block.setExtracted(basePixelMessage + leftPixelMessage + rightPixelMessage);
		return basePixelMessage + leftPixelMessage + rightPixelMessage;
	}
	
	public static String foo(int size, String msg) {
		while(msg.length() != size) {
			msg = '0'+msg;
		}
		return msg;
	}
	
	public static void printBlockInfo(List<Block> blocks, List<Block> embeddedBlocks, String name) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter
				(new FileOutputStream(name+".txt"), "utf-8"));
		for(int i = 0; i < blocks.size(); i++) {
			writer.write("Block "+i+" "+blocks.get(i).getLeftPixel()+" "+blocks.get(i).getBasePixel()+" "+blocks.get(i).getRightPixel());
			writer.newLine();
			writer.write("Embeddable: L "+blocks.get(i).getLeftRange()+" R "+blocks.get(i).getRightRange());
			writer.newLine();
			writer.write("Embedded:  "+blocks.get(i).getEmbedded());
			writer.newLine();
			writer.write("Extracted: "+embeddedBlocks.get(i).getExtracted());
			writer.newLine();
		}
		writer.flush();
		writer.close();
	}
	
	public static String testing(List<Block> blocks, List<Block> embeddedBlocks) {
		int i = 1;
		while(!blocks.get(i).getEmbedded().isEmpty() && !embeddedBlocks.get(i).getExtracted().isEmpty()) {
			if(!blocks.get(i).getEmbedded().equals(embeddedBlocks.get(i).getExtracted())){
				System.out.println(i);
				i++;
			}
		}
		return Integer.toString(i);
	}

	public int[][] getImageGrid() {
		return imageGrid;
	}

	public void setImageGrid(int[][] imageGrid) {
		this.imageGrid = imageGrid;
	}

	public MessageHelper getSecretMessage() {
		return secretMessage;
	}

	public void setSecretMessage(MessageHelper secretMessage) {
		this.secretMessage = secretMessage;
	}
	
//	public static void main(String[] args) throws IOException {
//		int[][] rangeTable = 	{{0,8,16,32,64}, 	//lj
//							     {7,15,31,63,255}, 	//uj
//								 {0,0,0,0,0}};		//tj
//		//input
//		BufferedImage image = ImageHelper.getImage("lena_gray.bmp");
//		
//		int[][] stegoGrid = new int[image.getHeight()][image.getWidth()];	//initialize array
//		stegoGrid = ImageHelper.getImagePixelValues(image, stegoGrid); 	//module 2 || [col][row]
//		MessageHelper secretMessage = new MessageHelper("toEmbed.txt"); 	//module 3
//		
//		embedTableClue(stegoGrid[0][0], imageClassification(stegoGrid)); 	//preprocess, true if smooth, else false
//		
//		List<Block> blocks = ImageHelper.pixelDivision(stegoGrid);						//pixel division
//		rangeTable[2] = getEmbeddableBits(rangeTable);
//		int gg = 0;
//		for(int i = 0; i < blocks.size(); i++){			//embedding phase
//			if(secretMessage.getCurrentBit() <= secretMessage.getFinalBit()){
//				embedBlock(blocks.get(i), secretMessage, rangeTable);
//				gg++;
//			}
//		}
//		updateGrid(stegoGrid, blocks);
//		ImageHelper.createStegoImage(stegoGrid, "ARCStegoImage");
//		
//		//extraction
//		BufferedImage stegoImage = ImageHelper.getImage("stegoImage.bmp");
//		int[][] embeddedStegoGrid = new int[stegoImage.getHeight()][stegoImage.getWidth()];
//		embeddedStegoGrid = ImageHelper.getImagePixelValues(stegoImage, embeddedStegoGrid);
//		List<Block> embeddedBlocks = ImageHelper.pixelDivision(embeddedStegoGrid);
//		
//		String embeddedSecretMessage = "";
//		for(int i = 0; i < gg; i++) { //TODO when to stop
////			System.out.println(i);
//			embeddedSecretMessage += extractBlock(blocks.get(i), rangeTable);
//		}
//		printBlockInfo(blocks, blocks, "embeddedBlocks");
//		//testing(blocks, embeddedBlocks);
//		
//		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter
//				(new FileOutputStream("secMess.txt"), "utf-8"));
//		writer.write(embeddedSecretMessage);
//		writer.flush();
//		writer.close();
//		
//		System.out.println(MessageHelper.binaryToASCII("secMess.txt"));
//	}
}
