# Project Context
Live services genrally generate logs at a very high rate; e.g., services creates over 100,000 log lines a second.
Usually, these logs are loaded inside databases to enable fast querying, but the cost of keeping all the
logs becomes too high. For this reason, only the recent logs are kept in databases, and logs for longer
periods are kept in file archives.
For this problem, we should assume we store our data in multiple files. We close a file and start a new
file when the file size reaches X (X could be in GBs) . Our file names are of the format LogFile-######.log (e.g., LogFile-
000008.log, or LogFile-000139.log). There could be thousands of log files and each could be as large as X in size.

# Execuation
Assuming Every log line will start with TimeStamp in "ISO 8601" format followed by a comma (',').
Example logline:
2020-01-31T20:12:38.1234Z, Some Field, Other Field, And so on, Till new line,...\n

The command line (CLI) for the desired program is as below
> LogExtractor.exe -f "From Time" -t "To Time" -i "Log file directory location" 

#### Time Complexity:   2*O(log(N)) + O(log(M))

Where, N = Total No. of Files in the specified directory and
       M = Total no. of lines in the file.

Since M>>>>>N

#### Overall Complexity: O(log(M))

