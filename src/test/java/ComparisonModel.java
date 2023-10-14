import org.testng.annotations.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.HashMap;
import javax.imageio.ImageIO;
import static com.github.romankh3.image.comparison.ImageComparisonUtil.pixelDiff;

public class ComparisonModel {

    static FileWriter fileWriter;
    static {
        try {
            fileWriter = new FileWriter("Difference.txt");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ComparisonModel() throws IOException {
    }

    static PrintWriter printWriter = new PrintWriter(fileWriter);
    static HashMap<String, Float> diffMap = new HashMap<>();
    static HashMap<String, Integer> heightDiffMap = new HashMap<>();


    @Test
    public void comparison() throws IOException {
        // Folder paths for the screenshots to be compared
        String folder1 = "ProdShots";
        String folder2 = "LowerShots";

        // Create a folder for the resulting images
        String resultFolder = "ResultedImage";
        File folder = new File(resultFolder);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        // List all the files in the first folder
        File[] files1 = new File(folder1).listFiles();

        // Iterate through each file in the first folder
        for (File file1 : files1) {
            // Get the file name without the extension
            String fileName = file1.getName().replaceFirst("[.][^.]+$", "");

            // Construct the corresponding file path in the bait folder
            String folder2FilePath = folder2 + "/" + file1.getName();
            File file2 = new File(folder2FilePath);

            if (file2.exists()) {
                // Read the image file from the first folder
                BufferedImage img1 = ImageIO.read(file1);

                // Read the image file from the second folder
                BufferedImage img2 = ImageIO.read(new File(folder2 + "/" + file1.getName()));

                // Compare the images and get the difference image
                BufferedImage diffImg = getDifferenceImage(img1, img2, fileName);

                // Save the difference image in the result folder
                String resultFileName = resultFolder + "/Resulted" + fileName + ".png";
                ImageIO.write(diffImg, "png", new File(resultFileName));
            }
        }

        for (String fileName : diffMap.keySet()) {
            float difference = diffMap.get(fileName);
            int heightDiff = heightDiffMap.get(fileName);  // Get the height difference
            printWriter.println(fileName + ": Difference=" + difference + ", Height Difference=" + heightDiff);
        }
        printWriter.close();
    }

    @Test
    public static float getDifferencePercent(BufferedImage img1, BufferedImage img2) throws IOException {
        int width = img1.getWidth();
        int height = img1.getHeight();

        long diff = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                diff += pixelDiff(img1.getRGB(x, y), img2.getRGB(x, y));
            }
        }
        long maxDiff = 3L * 255 * width * height;

        return (float) (100.0 * diff / maxDiff);
    }

    @Test
    public static BufferedImage getDifferenceImage(BufferedImage img1, BufferedImage img2, String fileName) throws IOException {

        int heightDiff = Math.abs(img1.getHeight() - img2.getHeight());
        heightDiffMap.put(fileName, heightDiff);

        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) {
            // Resize the images to the same dimensions
            int width = Math.min(img1.getWidth(), img2.getWidth());
            int height = Math.min(img1.getHeight(), img2.getHeight());
            Image scaledImg1 = img1.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            Image scaledImg2 = img2.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            img1 = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            img2 = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d1 = img1.createGraphics();
            Graphics2D g2d2 = img2.createGraphics();
            g2d1.drawImage(scaledImg1, 0, 0, null);
            g2d2.drawImage(scaledImg2, 0, 0, null);
            g2d1.dispose();
            g2d2.dispose();
        }

        // Create a new image with the same dimensions as the input images
        BufferedImage diffImg = new BufferedImage(img1.getWidth(), img1.getHeight(), BufferedImage.TYPE_INT_ARGB);

        // Compare the pixels of the two images
        float difference = getDifferencePercent(img1, img2);
        diffMap.put(fileName, difference);

        for (int x = 0; x < img1.getWidth(); x++) {
            for (int y = 0; y < img1.getHeight(); y++) {
                int rgb1 = img1.getRGB(x, y);
                int rgb2 = img2.getRGB(x, y);

                // If the pixels are not the same, set the pixel in the difference image to red
                if (rgb1 != rgb2) {
                    diffImg.setRGB(x, y, 0xFFFF0000);
                } else {
                    diffImg.setRGB(x, y, rgb1);
                }
            }
        }

        return diffImg;
    }
}
