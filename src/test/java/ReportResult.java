import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.testng.annotations.Test;

import java.io.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReportResult {

    @Test
    public void genReport() throws IOException {
        File reportFile = new File("Difference.txt");

        // Reader object for the input file
        BufferedReader reportReader = new BufferedReader(new FileReader(reportFile));

        // CSVPrinter object for the output file
        CSVPrinter printer = new CSVPrinter(new FileWriter("Report.csv"), CSVFormat.DEFAULT);

        // Object for timestamp
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        // S is the millisecond
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy' 'HH:mm:ss:S");

        String regex = "^([^:]+):\\s+(?:[^=]+?)=([\\d\\.]+),\\s+(?:[^=]+?)=([\\d\\.]+)";
        // Write the headers for the CSV file
        printer.printRecord("Filename", "Difference Percentage", "Height Mismatch", "Timestamp");

        // Read and write the contents of the report file
        String line;
        while ((line = reportReader.readLine()) != null) {
            // Split the line into filename, difference percentage, and height difference
            Matcher matcher = Pattern.compile(regex).matcher(line);
            if (matcher.matches()) {
                String fileName = matcher.group(1).trim();
                String difference = matcher.group(2).trim();
                String heightMismatch = matcher.group(3).trim();
                String timeStamp = simpleDateFormat.format(timestamp);
                printer.printRecord(fileName, difference, heightMismatch, timeStamp);
            }
        }

        // Close the reader and printer
        reportReader.close();
        printer.close();
    }
}
