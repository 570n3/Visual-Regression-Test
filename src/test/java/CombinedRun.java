import java.io.*;

public class CombinedRun {

    public static void main(String[] args) throws InterruptedException, IOException {

        ProdShots images = new ProdShots();
        LowerShots lowerImages = new LowerShots();
        ReportResult report = new ReportResult();

        images.imageShots();
        lowerImages.imageShots();
        report.genReport();


    }
}
