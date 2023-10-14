import java.io.File;

public class FileRenamer {
    public static void main(String[] args) {
        String folderPath = "BaitFolder"; // Specify the folder path here
        String textToRemove = "_content_example_global"; // Specify the text to remove here

        File folder = new File(folderPath);
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String fileName = file.getName();
                    String newFileName = fileName.replace(textToRemove, "");

                    File newFile = new File(folderPath + File.separator + newFileName);
                    file.renameTo(newFile);

                }
            }
        }
    }
}

