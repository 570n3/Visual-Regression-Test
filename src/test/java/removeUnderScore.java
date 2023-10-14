import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class removeUnderScore {

    public static void replaceUnderscoreWithSlash(String filePath) {
        String line;
        String csvSplitBy = ",";

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            // Create a new CSV file for writing the modified data
            FileWriter writer = new FileWriter("FinalReport.csv");

            while ((line = br.readLine()) != null) {
                // Split the line by the comma delimiter
                String[] columns = line.split(csvSplitBy);

                // Replace underscore with slash in the first column
                String modifiedColumn = columns[0].replace("_", "/");

                // Write the modified column and the remaining columns to the new CSV file
                writer.append(modifiedColumn);
                for (int i = 1; i < columns.length; i++) {
                    writer.append(",");
                    writer.append(columns[i]);
                }
                writer.append("\n");
            }

            writer.flush();
            writer.close();

            System.out.println("CSV file processed successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String filePath = "Report.csv";
        replaceUnderscoreWithSlash(filePath);
    }
}

