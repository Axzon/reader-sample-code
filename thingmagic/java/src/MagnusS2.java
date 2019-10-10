import com.thingmagic.*;

public class MagnusS2 {

    /**
     * Tag Settings
     * 
     * Read Attempts: number of tries to read all nearby sensor tags
     * 
     * On-Chip RSSI Filters: sensor tags with on-chip RSSI codes outside
     * of these limits won't respond. 
     * 
     * Mode: select which sensor to use
     * - Moisture: 'true'
     * - On-Chip RSSI: 'false'
     */
    static int readAttempts = 10;
    static byte ocrssiMin = 3;
    static byte ocrssiMax = 31;
    static boolean moistureMode = true;
    
    public static void main(String[] args) {
        try {
            // connect to and initialize reader
            Reader reader = Common.establishReader();
            
            // setup sensor activation commands and filters ensuring On-Chip RSSI Min Filter is applied
            Gen2.Select reset = Common.createGen2Select(4, 5, Gen2.Bank.TID, 0x00, 24, new byte[] { (byte)0xE2, (byte)0x82, (byte)0x40 });
            Gen2.Select ocrssiMinFilter = Common.createGen2Select(4, 0, Gen2.Bank.USER, 0xA0, 8, new byte[] { (byte)(0x20 | (ocrssiMin - 1)) });
            Gen2.Select ocrssiMaxFilter = Common.createGen2Select(4, 2, Gen2.Bank.USER, 0xA0, 8, new byte[] { ocrssiMax });
            MultiFilter selects = new MultiFilter(new Gen2.Select[] { reset, ocrssiMinFilter, ocrssiMaxFilter });
            
            Gen2.ReadData operation;
            if (moistureMode){
                // read parameters for moisture code
                operation = new Gen2.ReadData(Gen2.Bank.RESERVED, 0xB, (byte)1);
            }
            else {
                // read parameters for on-chip RSSI code
                operation = new Gen2.ReadData(Gen2.Bank.RESERVED, 0xD, (byte)1);
            }
            
            // apply configuration
            SimpleReadPlan config = new SimpleReadPlan(Common.antennas, TagProtocol.GEN2, selects, operation, 1000);
            reader.paramSet(TMConstants.TMR_PARAM_READ_PLAN, config);
            
            for (int i = 1; i <= readAttempts; i++) {
                System.out.println("Read Attempt #" + i);
                
                // attempt sensor tag reading
                TagReadData[] results = reader.read(Common.readTime);
                
                if (results.length != 0) {
                    for (TagReadData tag: results) {
                        String epc = tag.epcString();
                        System.out.println("* EPC: " + epc);
                        short[] dataWords = Common.convertByteArrayToShortArray(tag.getData());
                        if (dataWords.length != 0) {
                            if (moistureMode) {
                                // Moisture Sensor
                                System.out.println("  - Moisture: " + dataWords[0] + " at " + tag.getFrequency() + " kHz");
                            }
                            else {
                                // On-Chip RSSI Sensor
                                System.out.println("  - On-Chip RSSI: " + dataWords[0]);
                            }                            
                        }
                    }
                }
                else {
                    System.out.println("No tag(s) found");
                }
                System.out.println();
            }
        }
        catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace(System.out);
            System.exit(-1);
        }
    }
}