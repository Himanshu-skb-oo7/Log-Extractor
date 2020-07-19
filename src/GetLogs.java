import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class GetLogs {
	private static BufferedReader br;

	public static void main(String[] args) throws ParseException, IOException {

		Date startDate = parseISODateString("2020-06-24T16:25:58.8450Z");
		Date endDate = parseISODateString("2020-06-24T16:26:00.8550Z");

		String path = "logs/2/";
		File directoryPath = new File(path);
		String files[] = directoryPath.list();
		Arrays.sort(files);

		int low = 0, high= files.length-1, mid= (low+high)/2, index=files.length;
	
		while(low<high) {
			mid = (low + high)/2;

			File file = new File(directoryPath+"/"+files[mid]);
			BufferedReader br = new BufferedReader(new FileReader(file));
			Date logEntryDate = parseISODateString(br.readLine().split(",")[0]);

			if(logEntryDate.before(startDate)) {
				if(low==mid) {
					index=high;
					break;
				}
				low=mid;
			} else {
				high=mid;
			}
			br.close();
		}
		
		int left=Math.max(index-1, 0), right;
		
		if(index==files.length) {
			right = left;
		} else {
			right = Math.max(findSecondInterchange(files, directoryPath, startDate, endDate, index+1, files.length) - 1, left);	
		}
		
//		System.out.println("left: "+left+" right: "+right);
//		System.exit(1);
		for(int i=left; i>=0 && i<=right; i++) {
			String str = null;
			File file = new File(directoryPath+"/"+files[i]);

			if(i==left || i==right) {
				//				printPartialLogFiles(files[i], directoryPath, startDate, endDate, (i==right));
				if(i==left) {
					printFirstFile(file, startDate, endDate);
				} else {
					printLastFile(file, startDate, endDate);
				}
			} else {
				br = new BufferedReader(new FileReader(file));

				while((str = br.readLine()) != null) {
					System.out.println(str);
				}
				
				br.close();
			}	
		}
	}

	static public void printPartialLogFiles(String fileName, File directoryPath, Date startDate, Date endDate, boolean lastFile) throws ParseException, IOException {

		File file = new File(directoryPath+"/"+fileName);
		RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
		String str = null;
		//		readFromLast(file, 4);
		long low=0, high=file.length(), index=-1;

		while(low<high) {
			long mid = (low+high)/2;
			//			System.out.println(mid);

			try {
				String dateTimeString = getTimstampBeforeNChars(file, mid).trim();
				Date logDate = parseISODateString(dateTimeString);	

				if(lastFile) {
					if(logDate.after(startDate)) {
						high = mid;
					} else {
						if(low==mid) {
							index=low;
							high--;
						}
						low = mid;
					}
				} else {
					if(logDate.before(endDate)) {
						if(low==mid) {
							high--;
						}
						low = mid;
					} else {
						high = mid;
					}
				}

				//				System.out.println(dateTimeString+" "+low+" "+high);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				break;
			}
		}
		randomAccessFile.seek(low+2);
		while((str=randomAccessFile.readLine()) != null) {
			Date logEntryDate = parseISODateString(str.split(",")[0]);
			if(logEntryDate.before(endDate) && logEntryDate.after(startDate)) {
				System.out.println(str);
			} else if(lastFile) {
				break;
			}
		}

	}

	static void printFirstFile(File file, Date startDate, Date endDate) throws IOException {
		long low=0, high=file.length(), index=0;

		while(low<high) {
			long mid = (low+high)/2;
			String dateTimeString = getTimstampBeforeNChars(file, mid).trim();
			Date logDate = parseISODateString(dateTimeString);

			if(logDate.after(startDate)) {
				high = mid;
			} else {
				if(low==mid) {
					index=low;
					break;
				}
				low = mid;
			}	
		}
		
		if(index != 0) {
			printPartialLogFile(file, startDate, endDate, index+2);	
		} else {
			printPartialLogFile(file, startDate, endDate, index);
		}
	}

	static void printLastFile(File file, Date startDate, Date endDate) throws IOException {
		printPartialLogFile(file, startDate, endDate, 0);	
	}

	static void printPartialLogFile(File file, Date startDate, Date endDate, long skipChars) throws IOException {
		RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
		randomAccessFile.seek(skipChars);

		String str;
		
		while((str=randomAccessFile.readLine()) != null) {
			Date logEntryDate = parseISODateString(str.split(",")[0]);
			if(logEntryDate.before(endDate) && logEntryDate.after(startDate)) {
				System.out.println(str);
			} else {
				break;
			}
		}

		randomAccessFile.close();
	}


	static public Date parseISODateString(String date) {
		return javax.xml.bind.DatatypeConverter.parseDateTime(date).getTime();
	}

	static public int findSecondInterchange(String[] files, File directoryPath, Date startDate, Date endDate, int low, int high) throws ParseException, IOException {
		int mid= (low+high)/2, index=files.length-1;

		while(low<high) {
			mid = (low + high)/2;

			File file = new File(directoryPath+"/"+files[mid]);
			BufferedReader br = new BufferedReader(new FileReader(file));
			Date logEntryDate = parseISODateString(br.readLine().split(",")[0]);

			if(logEntryDate.after(endDate)) {
				high=mid;
			} else {
				if(low==mid) {
					index=low;
					break;
				}
				low=mid;
			}
			
			br.close();
		}
		
		return index;
	}

	static public String getTimstampBeforeNChars(File file, long skipChars){
		StringBuilder builder = new StringBuilder();
		RandomAccessFile randomAccessFile = null;
		try {
			randomAccessFile = new RandomAccessFile(file, "r");
			randomAccessFile.seek(skipChars);

			long pointer = skipChars;

			while(pointer>=0 && (char)randomAccessFile.read() != '\n') {
				randomAccessFile.seek(pointer);
				pointer--;
			}
			//			System.out.println(skipChars+"  "+pointer);
			if(pointer!=0) {
				pointer++;
			}
			randomAccessFile.seek(pointer);

			char c;
			while(pointer < file.length() && (c=(char)randomAccessFile.read()) != ',') {
				builder.append(c);
				pointer++;
				randomAccessFile.seek(pointer);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}finally{
			if(randomAccessFile != null){
				try {
					randomAccessFile.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return builder.toString();
	}
}


