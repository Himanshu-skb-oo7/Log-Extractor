import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;


public class GenerateTestLogs {
	 static long LOG_FILE_SIZE_IN_MB;
	 static long TOTAL_LOGS_PER_DATE;
	 static Date FROM_DATE;
 	 static Date END_DATE;
 	 static String LOG_DIRECTORY;
 	
	 static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	 static Scanner scn = new Scanner(new InputStreamReader(System.in));
	 
	 public static void main(String args[]) throws IOException, ParseException {  
	     int totalFilesProcessed = 0;
	     
	     getInputs();
	     
	     Date currentDate = FROM_DATE;

    	 long logsProcessed = 0;
    	 
	     while(currentDate.before(END_DATE)) {
	    	 File logFile = getLogFile(totalFilesProcessed);
	    	 FileOutputStream outputStream = new FileOutputStream(logFile);
	    	 
	    	 while(getFileSizeInMB(logFile) < LOG_FILE_SIZE_IN_MB && currentDate.before(END_DATE)) {
	    		 
	    		 while(getFileSizeInMB(logFile) < LOG_FILE_SIZE_IN_MB && logsProcessed < TOTAL_LOGS_PER_DATE) {
		    		 String logEntry = getLogEntry(currentDate, logsProcessed + 1);
		    		 outputStream.write(logEntry.getBytes());
		    		 logsProcessed++;
		    	 }
	    		 
	    		 if(logsProcessed >= TOTAL_LOGS_PER_DATE) {
			    	 currentDate = addDays(currentDate, 1);
			    	 logsProcessed=0;
	    		 }
	    	
	    	 }
	    	
	    	 outputStream.close();
	    	 totalFilesProcessed++;
	     }
	 }
	 
	 static void getInputs() {
		 
		 System.out.println("Enter Log File Size in MB");
		 LOG_FILE_SIZE_IN_MB = getIntegerValue();

		 System.out.println("Enter Total Logs Per Date");
		 TOTAL_LOGS_PER_DATE = getIntegerValue();
		 
		 scn.nextLine();
		 
		 System.out.println("Enter the Date from which logs need to be generated (YYYY-MM-DD)");
		 FROM_DATE = getDate();
		 
		 System.out.println("Enter the Date from which logs need to be generated (YYYY-MM-DD)");
		 END_DATE = getDate();
		 
		 System.out.println("Enter directory where logs will be stored.");
		 LOG_DIRECTORY = scn.nextLine();
		 
		 new File(LOG_DIRECTORY).mkdir();

	 }
	 
	 static Date getDate() {
		 Date date;
		 try {
			 String dateString = scn.nextLine();
			 date = dateFormat.parse(dateString);
		 } catch (Exception e) {
			 System.out.print("Date must be in the format YYYY-MM-DD. Enter Again -:");
			 return getDate();
		 }
		 return date;
	 }
	 static long getIntegerValue() {
		 
		 long num=0;
		 try {
			 num = scn.nextLong();
		 } catch (Exception e) {
			 System.out.print("Invalid Format: It must be a Integer value. Enter Again -:");
			 scn.next();
			 return getIntegerValue();
		}
		return num;
	 }
	 
	 
	 static int getFileSizeInMB(File file) {
		 return (int) (file.length()*1.0/1024)/1024;
	 }
	 
	 static String getLogEntry(Date date, long logEntryForTheDate) {
		 String timezone = ZonedDateTime.now( ZoneOffset.UTC ).format(
				 DateTimeFormatter.ofPattern( "yyyy-MM-dd'T'HH:mm:ss.SSSS'Z'")
		 ).replaceAll("(.)*T", String.format("%sT", dateFormat.format(date)));
		 return String.format("%s, This is a Test Log. Entry Id for Today: [%d] generated at [%s] using GenerateTestLogs.java \n", timezone, logEntryForTheDate, timezone);
	 }
	 
	 static File getLogFile(int totalFilesProcessed) throws IOException {
		 File file =  new File(String.format("%s/LogFile-%d.log", LOG_DIRECTORY, (totalFilesProcessed + 1)));
		 file.createNewFile();
    	 return file;
	 }
	 
	 static Date addDays(Date date, int days) {
	        Calendar cal = Calendar.getInstance();
	        cal.setTime(date);
	        cal.add(Calendar.DATE, days); //minus number would decrement the days
	        return cal.getTime();
	 }
}
