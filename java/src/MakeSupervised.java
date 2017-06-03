import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class MakeSupervised {
    private static final String dir = "../drivers/features/";

    public static void main(String[] args) throws IOException {
        List<String> allData = new ArrayList<>();

        File[] drivers = new File(dir).listFiles();

        for (File driver : drivers) {
            BufferedReader reader = new BufferedReader(new FileReader(driver));
            reader.readLine(); // skip header

            String line;
            while ((line = reader.readLine()) != null) {
                allData.add(line);
            }
            reader.close();
        }
/*
        List<String> allData2 = new ArrayList<>(allData);
        for (int i = 1; i < 5; i++) {
            allData.addAll(allData2);
        }
        Collections.shuffle(allData, new Random(93501));

        Iterator<String> iterator = allData.iterator();
*/
        int[] rows = new int[5 * allData.size()];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = i;
        }
        List<Integer> list = Arrays.stream(rows).boxed().collect(Collectors.toList());
        Collections.shuffle(list, new Random(93501));

        Iterator<Integer> iterator = list.iterator();

        for (File driver : drivers) {
            String outputName = dir + driver.getName().split("\\.")[0] + "_sup.csv";
            BufferedReader reader = new BufferedReader(new FileReader(driver));
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputName));

            String line = reader.readLine();
            writer.write("label," + line + "\n");
            while ((line = reader.readLine()) != null) {
                writer.write("1," + line + "\n");
            }

            for (int i = 0; i < 1000; i++) {
                int row = iterator.next() % allData.size();
                writer.write("0," + allData.get(row) + "\n");
            }

            reader.close();
            writer.close();
        }
    }
}

