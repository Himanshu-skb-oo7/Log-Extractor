import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.ParseException;

public class GetLogs {
	private static final String FROM_DATE_FLAG = "f";
	private static final String TO_DATE_FLAG = "t";
	private static final String DIR_LOC_FLAG = "i";
	private static final String[] SUPPORTED_FILE_TYPES = { ".log" };

	private static BufferedReader br;
	private static Date from_date;
	private static Date to_date;
	private static File dir_loc;

	public static void main(String[] args) throws ParseException, IOException {

		long startTime = System.nanoTime();

		// Parsing the command line arguments to get Date Range and Dir Location
		parsingArguments(args);
		
		// Filter for getting only SUPPORTED FILES 
		FilenameFilter filterSupportedTypeFiles = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				for (String supportedFileType : SUPPORTED_FILE_TYPES) {
					if (name.endsWith(supportedFileType)) {
						return true;
					}
				}
				return false;
			}
		};

		// returns pathnames for files and directory
		String[] files = dir_loc.list(filterSupportedTypeFiles);

		// Avoid sorting in case there is not supported files in the directory.

		if (files == null) {
			throw new IllegalArgumentException("Specified directory not found.");
		}

		if (files.length != 0) {
			Arrays.sort(files); // sorting file order according to their names
		}

		int low = 0, high = files.length - 1, mid = (low + high) / 2, index = files.length;

		// Finding the file range that contains the desired log entries.
		while (low <= high) {
			mid = low + (high - low) / 2;
			File file = getFile(files[mid]);
			BufferedReader br = new BufferedReader(new FileReader(file));
			Date logEntryDate = parseISODateString(br.readLine().split(",")[0]);

			if (logEntryDate.before(from_date)) {
				low = mid + 1;
			} else {
				high = mid - 1;
				if (logEntryDate.before(to_date)) {
					index = mid;
				}
			}
			br.close();
		}

		int left = (index == files.length) ? high : Math.max(index - 1, 0);

		// Getting the right limit of files where desired logEntry could be present
		int right = Math.max(findSecondInterchange(files, left + 1, files.length - 1), left);

		for (int i = left; i >= 0 && i <= right; i++) {
			String str = null;
			File file = getFile(files[i]);

			if (i == left || i == right) {
				// Handling first and last files explicitly
				if (i == left) {
					printFirstFile(file);
				} else {
					printLastFile(file);
				}
			} else {
				// Getting file data into the buffer
				br = new BufferedReader(new FileReader(file));

				// Printing data until EOF
				while ((str = br.readLine()) != null) {
					System.out.println(str);
				}

				// close file connection
				br.close();
			}
		}

		long endTime = System.nanoTime();
		System.out.println(
				String.format("Query Successfully executed in %ss", (endTime - startTime) / (Math.pow(10, 9))));
	}

	static void processFlagValues(HashMap<String, String> flagsAndValues) {
		
		try {
			from_date = parseISODateString(flagsAndValues.get(FROM_DATE_FLAG));
		} catch (Exception e) {
			throw new IllegalArgumentException(String.format("Not a valid argument for -%s flag", FROM_DATE_FLAG));
		}

		try {
			to_date = parseISODateString(flagsAndValues.get(TO_DATE_FLAG));
		} catch (Exception e) {
			throw new IllegalArgumentException(String.format("Not a valid argument for -%s flag", TO_DATE_FLAG));
		}
		
		if(to_date.before(from_date)) {
			throw new IllegalArgumentException("End date should be earlier to the Start Date");
		}

		dir_loc = parseDirectoryLocation(flagsAndValues.get(DIR_LOC_FLAG));
		
	}

	static File parseDirectoryLocation(String dirLoc) throws IllegalArgumentException {

		if (dirLoc == null) {
			throw new IllegalArgumentException("Directory Location is required.");
		}

		return new File(dirLoc);
	}

	static void parsingArguments(String[] args) {

		HashMap<String, String> flagsAndValues = new HashMap<>();
		
		
		for (int i = 0; i < args.length; i++) {
			switch (args[i].charAt(0)) {
			case '-':
				if (args[i].length() != 2)
					throw new IllegalArgumentException("Not a valid argument: " + args[i]);
				else {
					if (args[i].charAt(1) == '-') {
						throw new IllegalArgumentException("Not a valid flag: " + args[i]);
					} else {
						if (args.length - 1 == i) {
							throw new IllegalArgumentException("Expecting an argument after " + args[i]);
						} else {
							flagsAndValues.put(args[i].substring(1), args[i + 1]);
							i++;
						}
					}
				}
				break;
			default:
				throw new IllegalArgumentException("Not a valid argument: " + args[i]);
			}
		}
		
		if(flagsAndValues.size() != 3) {
			throw new IllegalArgumentException("required arguments are missing.");
		}

		processFlagValues(flagsAndValues);
	}

	static void printFirstFile(File file) throws IOException {
		long low = 0, high = file.length();

		// Finding the First TimeStamp of the file which are in the given date range.
		while (low <= high) {
			long mid = low + (high - low) / 2;
			String dateTimeString = getTimstampBeforeNChars(file, mid).trim();
			try {
				Date logDate = parseISODateString(dateTimeString);

				if (logDate.after(from_date)) {
					high = mid - 1;
				} else {
					low = mid + 1;
				}
			} catch (IllegalArgumentException e) {
				// avoiding processing the file in case IllegalExpection occurred.
				break;
			}
		}

		// Handling final high value
		high = Math.max(high, 0);
		// Calling printPartialLogFile to print the logEntries after 'high' characters.
		printPartialLogFile(file, high);
	}

	static File getFile(String fileName) {
		return new File(dir_loc + "/" + fileName);
	}

	static void printLastFile(File file) throws IOException {
		// Calling printPartialLogFile for printing the logEntries.
		printPartialLogFile(file, 0);
	}

	static void printPartialLogFile(File file, long skipChars) throws IOException {
		// Getting to RandomAccessFile object for file processing
		RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");

		// Skipping the given characters
		randomAccessFile.seek(skipChars);

		String str;

		// Reading file until EOF
		while ((str = randomAccessFile.readLine()) != null) {
			try {
				// Parsing ISO DatTime String
				Date logEntryDate = parseISODateString(str.split(",")[0]);
				// Checking whether the log Entry lies between the given dateTime range
				if (logEntryDate.before(to_date) && logEntryDate.after(from_date)) {
					// Printing the LogEntry
					System.out.println(str);
				} else {
					// Break the loop as further log entries will not be in the date range
					break;
				}
			} catch (IllegalArgumentException e) {
				// Continue if there is IllegalArgumentException while parsing the date string
				continue;
			}
		}
		randomAccessFile.close();
	}

	static public Date parseISODateString(String date) {
		// Parsing ISO Date String to Date object
		return javax.xml.bind.DatatypeConverter.parseDateTime(date).getTime();
	}

	static public int findSecondInterchange(String[] files, int low, int high) throws ParseException, IOException {
		int mid = (low + high) / 2, index = -1;

		// Finding the last file which can contains the log entry in the given date
		while (low <= high) {
			mid = low + (high - low) / 2;

			File file = getFile(files[mid]);
			BufferedReader br = new BufferedReader(new FileReader(file));
			Date logEntryDate = parseISODateString(br.readLine().split(",")[0]);

			if (logEntryDate.after(to_date)) {
				high = mid - 1;
			} else {
				if (logEntryDate.after(from_date)) {
					index = mid;
				}
				low = mid + 1;
			}
			br.close();
		}
		return index;
	}

	static public String getTimstampBeforeNChars(File file, long skipChars) {
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
			while (pointer >= 0 && (char) randomAccessFile.read() != '\n') {
				randomAccessFile.seek(pointer);
				pointer--;
			}

			// Handling pointer at end of the line
			if (pointer != 0) {
				pointer++;
			}
			randomAccessFile.seek(pointer);

			char c;
			// Reading file forward until ',' encountered
			while (pointer < file.length() && (c = (char) randomAccessFile.read()) != ',') {
				// Appending each character to the builder to get full TimeStamp string
				builder.append(c);
				pointer++;
				randomAccessFile.seek(pointer);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// Closing the File
			if (randomAccessFile != null) {
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
