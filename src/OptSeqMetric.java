import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;


public class OptSeqMetric {
	public HashMap<String, Integer> hashOptBitToLine;
	public HashMap<String, String> hashOptStrToMetric;
	
	OptSeqMetric() {
		hashOptBitToLine = new HashMap<String, Integer>();
		hashOptStrToMetric = new HashMap<String, String>();
	}
	
	
	public class OptSequence {
		public int fuse;    // 0: not applied, 001 Nofuse 010 Smartfuse 100 Maxfuse 
		public int unroll;  // 0: not applied, 0001 u2 0010 u4 0100 u6  1000 u8 
		public boolean vectorized; // vectorized or not
		public boolean tiled;      // tiled or not
		public int level;          // number of levels nested (2,3,4 for polybench
		public int tileSizes[];
		// construct 
		OptSequence(String seq) {
			if (!seq.contains("fuse")) {
				fuse = 0;
			}
			if (seq.contains("nofuse")) {
				fuse = 1;
			}
			if (seq.contains("smartfuse")) {
				fuse = 2;
			}
			if (seq.contains("maxfuse")) {
				fuse = 3;
			}
			
			if (!seq.contains("ufactor")) {
				unroll = 0;
			}
			if (seq.contains("ufactor_2")) {
				unroll = 1;
			}
			if (seq.contains("ufactor_4")) {
				unroll = 2;
			}
			if (seq.contains("ufactor_6")) {
				unroll = 3;
			}
			if (seq.contains("ufactor_8")) {
				unroll = 4;
			}
			
			// vectorization
			if (!seq.contains("vector")) {
				vectorized = false;
			}
			else {
				vectorized = true;
			}
			
			if (!seq.contains("tile")) {
				tiled = false;
			}
			
			// tiled, determine level of tiling and what tile size was used in each level
			else {
				tiled = true;   // first set the tiled bit
				
				// deal with tiling, using regular expression to determine level
				// pay attention to precedence!
				if (seq.matches("(.*)tile(.*)x(.*)x(.*)x(.*)")) {
					level = 4;					
				}
				else if (seq.matches("(.*)tile(.*)x(.*)x(.*)")) {
					level = 3;
				}
				else if (seq.matches("(.*)tile(.*)x(.*)")) {
					level = 2;
				}
				tileSizes = new int[level];
				
				//extract detailed tiling size 
				int start = seq.indexOf("tile_");
				int end = seq.indexOf(".c");
				String tileInfo = seq.substring(start+5, end);
				String[] temp = tileInfo.split("x");
				for (int i=0; i<level; i++) {
					tileSizes[i] = Integer.parseInt(temp[i]);
				}
			}
		}
		public String getEncodingString() {
			StringBuilder sb = new StringBuilder();
			switch (fuse) {
				case 0: sb.append("0 0 0 "); break;
				case 1: sb.append("0 0 1 "); break;
				case 2: sb.append("0 1 0 "); break;
				case 3: sb.append("1 0 0 "); break;
			}
			
			switch (unroll) {
				case 0: sb.append("0 0 0 0 "); break;
				case 1: sb.append("0 0 0 1 "); break;
				case 2: sb.append("0 0 1 0 "); break;
				case 3: sb.append("0 1 0 0 "); break;
				case 4: sb.append("1 0 0 0 "); break;
			}
			
			if (vectorized) {
				sb.append("1 ");
			}
			else {
				sb.append("0 ");
			}
			
			// deal with no-tiling print
			if (!tiled) {
				sb.append("0 ");
				//append 4 levels of 0s.
				for (int i=0; i<4; i++) {
					sb.append("0 0 0 0 ");
				}
			}
			else {
				sb.append("1 ");
				
				for (int i=0; i<level; i++) {
					switch(tileSizes[i]) {
						case 1: sb.append("0 0 0 1 "); break;
						case 16: sb.append("0 0 1 0 "); break;
						case 32: sb.append("0 1 0 0 "); break;
						case 64: sb.append("1 0 0 0 "); break;
					}	
				}
				
				for (int i=level; i<4; i++) {
					sb.append("0 0 0 0 ");
				}
			}
			
			
			return sb.toString();
		}
	}	

	
	public void constructOptBitToLineNumberMapping(String optbitFile) {
		String line = new String();
	    try {

	        // Always wrap FileReader in BufferedReader.
	        // File should be under workspace folder name directory
	        BufferedReader bufferedReader = 
	            new BufferedReader(new FileReader(optbitFile));
	        int linenumber = 1;
	        while((line = bufferedReader.readLine()) != null) {
	        	hashOptBitToLine.put(line, linenumber);
	        	linenumber++;
	        }  
	        // Always close files.
	        bufferedReader.close();      
	     }
	        catch(FileNotFoundException ex) {
	            System.out.println(
	                "Unable to open file '" + 
	                    optbitFile + "'");        
	        }
	       catch(IOException ex) {
	            System.out.println(
	                "Error reading file '" 
	                + optbitFile + "'");          
	            // Or we could just do this: 
	            // ex.printStackTrace();
	        }
		
	}
	
	public void constructOptStrToMetricMapping(String uniqWithMetricFile) {
		String line = new String();
	    try {

	        // Always wrap FileReader in BufferedReader.
	        // File should be under workspace folder name directory
	        BufferedReader bufferedReader = 
	            new BufferedReader(new FileReader(uniqWithMetricFile));
	        while((line = bufferedReader.readLine()) != null) {
	        	//omit .c 
	        	String [] split = line.split(" ");
	        	
	        	int end = split[0].indexOf(".XLoutput");
	        	split[0] = split[0].substring(0,end);
	        	
	        	hashOptStrToMetric.put(split[0], split[1]+" "+split[2]+" "+split[3]);
	        	
	        }  
	        // Always close files.
	        bufferedReader.close();      
	     }
	        catch(FileNotFoundException ex) {
	            System.out.println(
	                "Unable to open file '" + 
	                    uniqWithMetricFile + "'");        
	        }
	       catch(IOException ex) {
	            System.out.println(
	                "Error reading file '" 
	                + uniqWithMetricFile + "'");          
	            // Or we could just do this: 
	            // ex.printStackTrace();
	        }	    		
	}
	
	public void interpretMappingAndInstallOptSeqMetric(String mappingFile) {
		String line = new String();
		
	    try {
	        // Always wrap FileReader in BufferedReader.
	        // File should be under workspace folder name directory
	        BufferedReader bufferedReader = 
	            new BufferedReader(new FileReader(mappingFile));
	        while((line = bufferedReader.readLine()) != null) {
	        	String[] split = line.split(" ");

	        	OptSequence cur = new OptSequence(split[2]);
	        	System.out.print(hashOptBitToLine.get( cur.getEncodingString()) + " ");
	        	
	        	System.out.print( hashOptStrToMetric.get(split[3]) + " " );
	        	
	        	//System.out.print(line);	        	
	        	System.out.println();
	        }  
	        // Always close files.
	        bufferedReader.close();      
	     }
	        catch(FileNotFoundException ex) {
	            System.out.println(
	                "Unable to open file '" + 
	                    mappingFile + "'");        
	        }
	       catch(IOException ex) {
	            System.out.println(
	                "Error reading file '" 
	                + mappingFile + "'");          
	            // Or we could just do this: 
	            // ex.printStackTrace();
	        }	    		
	}
	
	public void getOptSeqMetric(String file1, String file2, String file3) {
		
		constructOptBitToLineNumberMapping(file1);

		constructOptStrToMetricMapping(file2);
		
		interpretMappingAndInstallOptSeqMetric(file3);
	}
	 
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if (args.length != 3) {
			System.out.println("java -classpath bin/ OptSeqMetric: param1 param2 param3");
			System.out.println("param1: optbit");
			System.out.println("param2: 2mm-result.txt");
			System.out.println("param3: 2mm-diff-result");
		}
		
		OptSeqMetric solve = new OptSeqMetric();
		solve.getOptSeqMetric(args[0], args[1], args[2]);
	}

}
