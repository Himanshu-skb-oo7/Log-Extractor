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

		Date startDate = parseISODateString("2020-06-17T16:56:51.5970Z");
		Date endDate = parseISODateString("2020-07-17T16:56:51.6000Z");

		String path = "logs/2/";
		File directoryPath = new File(path);
		String files[] = directoryPath.list();
		Arrays.sort(files);

		int low = 0, high= files.length-1, mid= (low+high)/2, index=files.length;

		while(low<=high) {
			mid = low + (high - low) / 2; 
			File file = new File(directoryPath+"/"+files[mid]);
			BufferedReader br = new BufferedReader(new FileReader(file));
			Date logEntryDate = parseISODateString(br.readLine().split(",")[0]);

			if(logEntryDate.before(startDate)) {
				if(low==mid) {
					break;
				}
				low=mid+1;
			} else {
				high=mid-1;
				if(logEntryDate.before(endDate)) {
					index=mid;
				}
			}
			br.close();
		}

		int left= (index == files.length) ?  high : Math.max(index-1, 0);

		int right = Math.max(findSecondInterchange(files, directoryPath, startDate, endDate, left+1, files.length - 1), left);	
		
		for(int i=left; i>=0 && i<=right; i++) {
			String str = null;
			File file = new File(directoryPath+"/"+files[i]);

			if(i==left || i==right) {
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

	static void printFirstFile(File file, Date startDate, Date endDate) throws IOException {
		long low=0, high=file.length(), index=0;

		while(low<=high) {
			long mid = low + (high-low)/2;
			String dateTimeString = getTimstampBeforeNChars(file, mid).trim();
			try {
				Date logDate = parseISODateString(dateTimeString);

				if(logDate.after(startDate)) {
					high = mid-1;
				} else {
					low = mid+1;
				}
			} catch (IllegalArgumentException e) {
				break;
			}
		}
		
		high = Math.max(high, 0);

		if(high != 0) {
			printPartialLogFile(file, startDate, endDate, high+2);	
		} else {
			printPartialLogFile(file, startDate, endDate, high);
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
		int mid= (low+high)/2, index=-1;

		while(low<=high) {
			mid = low + (high - low)/2;

			File file = new File(directoryPath+"/"+files[mid]);
			BufferedReader br = new BufferedReader(new FileReader(file));
			Date logEntryDate = parseISODateString(br.readLine().split(",")[0]);

			if(logEntryDate.after(endDate)) {
				high=mid-1;
			} else {
				if(logEntryDate.after(startDate)) {
					index=mid;
				}
				low=mid+1;
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


