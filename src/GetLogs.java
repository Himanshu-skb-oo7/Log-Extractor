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

		Date startDate = parseISODateString("2020-06-17T00:56:51.5970Z");
		Date endDate = parseISODateString("2020-06-17T23:56:51.6000Z");

		String path = "logs/2/";
		File directoryPath = new File(path);
		String files[] = directoryPath.list();

		Arrays.sort(files); // sorting file order according to their names

		int low = 0, high= files.length-1, mid= (low+high)/2, index=files.length;
		
		// Finding the file range that contains the desired log entries using Binary Search.
		while(low<=high) {
			mid = low + (high - low) / 2; 
			File file = new File(directoryPath+"/"+files[mid]);
			BufferedReader br = new BufferedReader(new FileReader(file));
			Date logEntryDate = parseISODateString(br.readLine().split(",")[0]);

			if(logEntryDate.before(startDate)) {
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

		// Getting the right limit of files where desired logEntry could be present
		int right = Math.max(
				findSecondInterchange(files, directoryPath, startDate, endDate, left+1, files.length - 1), 
				left
				);	

		for(int i=left; i>=0 && i<=right; i++) {
			String str = null;
			File file = new File(directoryPath+"/"+files[i]);

			if(i==left || i==right) {
				// Handling first and last files explicitly
				if(i==left) {
					printFirstFile(file, startDate, endDate);
				} else {
					printLastFile(file, startDate, endDate);
				}
			} else {
				// Getting file data into the buffer
				br = new BufferedReader(new FileReader(file));

				// Printing data until EOF
				while((str = br.readLine()) != null) {
					System.out.println(str);
				}

				// close file connection
				br.close();
			}	
		}
	}

	static void printFirstFile(File file, Date startDate, Date endDate) throws IOException {
		long low=0, high=file.length();

		// Finding the First TimeStamp of the file which are in the given dateTime range using Binary Search
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
				// avoiding processing the file in case IllegalExpection occurred.
				break;
			}
		}

		// Handling final high value
		high = Math.max(high, 0);
		// Calling printPartialLogFile to print the logEntries after 'high' characters.
		printPartialLogFile(file, startDate, endDate, high);	
	}

	static void printLastFile(File file, Date startDate, Date endDate) throws IOException {
		// Calling printPartialLogFile for printing the logEntries.
		printPartialLogFile(file, startDate, endDate, 0);	
	}

	static void printPartialLogFile(File file, Date startDate, Date endDate, long skipChars) throws IOException {
		// Getting to RandomAccessFile object for file processing
		RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");

		// Skipping the given characters
		randomAccessFile.seek(skipChars);

		String str;

		// Reading file until EOF
		while((str=randomAccessFile.readLine()) != null) {
			try {
				// Parsing ISO DatTime String 
				Date logEntryDate = parseISODateString(str.split(",")[0]);
				// Checking whether the log Entry lies between the given dateTime range
				if(logEntryDate.before(endDate) && logEntryDate.after(startDate)) {
					// Printing the LogEntry
					System.out.println(str);
				} else {
					// Breaking the loop to read file further as current log entry does not lies between the range
					break;
				}	
			} catch (IllegalArgumentException e) {
				// Continue to read file if there is IllegalArgumentException while parsing the date string 
				continue;
			}	
		}
		randomAccessFile.close();
	}


	static public Date parseISODateString(String date) {
		// Parsing ISO Date String to Date object
		return javax.xml.bind.DatatypeConverter.parseDateTime(date).getTime();
	}

	static public int findSecondInterchange(String[] files, File directoryPath, Date startDate, Date endDate, int low, int high) throws ParseException, IOException {
		int mid= (low+high)/2, index=-1;

		// Finding the File after which No file contains the log entry in the given date range
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
		// Creating StringBuilder object for storing the TimeStamp purpose 
		StringBuilder builder = new StringBuilder();
		RandomAccessFile randomAccessFile = null;
		try {
			// Creating RandomAccessFile object for reading the file 
			randomAccessFile = new RandomAccessFile(file, "r");
			// Skipping the given characters
			randomAccessFile.seek(skipChars);

			long pointer = skipChars;

			// Move pointer backward while we encounter End of the Line
			while(pointer>=0 && (char)randomAccessFile.read() != '\n') {
				randomAccessFile.seek(pointer);
				pointer--;
			}

			// If pointer is not at beginning of the file the move it to next character as current character is EOL
			if(pointer!=0) {
				pointer++;
			}
			randomAccessFile.seek(pointer);

			char c;
			// Reading file forward until ',' encountered
			while(pointer < file.length() && (c=(char)randomAccessFile.read()) != ',') {
				// Appending each character to the builder to get full TimeStamp string
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
			// Closing the File
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


