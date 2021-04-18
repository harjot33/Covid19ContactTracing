import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


public class MobileDevice {
    Properties configFile;
    Government contactTracer;
    static HashMap<String, Integer> mobileRegister = new HashMap<>();
    HashMap<Integer, Properties> contactList = new HashMap<>();
    int contact_instance=0;
    int synchchecker=0;
    HashMap<String, ArrayList<String >> testrecord = new HashMap<>();
    ArrayList<String> TestList = new ArrayList<>();
    String initiator = "";

    MobileDevice(Properties configFile, Government contactTracer)  {
            this.configFile = configFile;
            initiator = configFile.getProperty("deviceHash");
            this.contactTracer = contactTracer;

    }


    public void recordContact(String individual, int date, int duration){

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(sdf.parse("01-01-2021"));
        }catch (ParseException pe){
            System.out.println(pe);
        }

        calendar.add(Calendar.DAY_OF_MONTH,date);
        String dateofcontact = sdf.format(calendar.getTime());
        Properties p = new Properties();
        p.setProperty("deviceHash",individual);
        p.setProperty("date",dateofcontact);
        p.setProperty("duration",""+duration);
        contactList.put(contact_instance,p);
        contact_instance ++;
        System.out.println("DEVICE NAME- "+configFile.getProperty("deviceName"));
        System.out.println(contactList);

    }

    public void positiveTest(String testHash) {
        if(testHash.isEmpty() || testHash==null){
            System.out.println("Invalid Test Hash Entered.");
            return;
        }
        String deviceHash = configFile.getProperty("devHash");
        if (!TestList.contains(testHash)) {
            TestList.add(testHash);
            testrecord.put(deviceHash, TestList);
        } else {
            System.out.println("Test has already been reported.");
        }

    }

    public boolean synchronizeData() throws Exception {
        DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance(); // Using the DOM DocumentBuilderFactory's instance to create a documentfactory object
        DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder(); // We create a new documentBuilder
        Document document = documentBuilder.newDocument();
        Element information = document.createElement("information"); //We create the root element of the document tree
        document.appendChild(information); // We append it to the document tree as the root child

        Element sourcedevice_hash = document.createElement("sourceHash"); //We create the root element of the document tree
        sourcedevice_hash.appendChild(document.createTextNode(configFile.getProperty("deviceHash"))); // We set the child of this node as a textnode, depicting some text value
        information.appendChild(sourcedevice_hash); // We append it to the document tree as the root child

        Element test_Hash = document.createElement("testHash"); //We create the root element of the document tree
        for (int i = 0 ; i<TestList.size() ; i++) {
            test_Hash.appendChild(document.createTextNode(TestList.get(i))); // We set the child of this node as a textnode, depicting some text value
        }
        information.appendChild(test_Hash); // We append it to the document tree as the root child

        Element contacts_list = document.createElement("contact_list"); //We create the root element of the document tree
        information.appendChild(contacts_list); // We append it to the document tree as the root child
        for(int i = 0 ; i<contactList.size();i++){
            Element device = document.createElement("device");
            device.appendChild(document.createTextNode(i+""));
            contacts_list.appendChild(device);
            Element deviceHash = document.createElement("deviceHash");
            deviceHash.appendChild(document.createTextNode(contactList.get(i).getProperty("deviceHash")));
            device.appendChild(deviceHash);
            Element date = document.createElement("date");
            date.appendChild(document.createTextNode(contactList.get(i).getProperty("date")));
            device.appendChild(date);
            Element duration = document.createElement("duration");
            duration.appendChild(document.createTextNode(contactList.get(i).getProperty("duration")));
            device.appendChild(duration);
        }


        Transformer transformer = TransformerFactory.newInstance().newTransformer(); // Initializing Transformer Object with an instance of TransformerFactory
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        String contactInfo = writer.getBuffer().toString();
        contactTracer.mobileContact(initiator,contactInfo);
        return true;
    }

    private static String HashGenerator(String deviceName) throws NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] sha256 = md.digest(deviceName.getBytes(StandardCharsets.UTF_8));
        BigInteger sig = new BigInteger(1, sha256);
        String deviceHash = sig.toString(16);
        //System.out.println(deviceHash);
        return deviceHash;
    }




    public static void main(String[] args) throws Exception {

        Properties govtproperties = new Properties();
        govtproperties.setProperty("user","harjot");
        govtproperties.setProperty("password","B00872298");
        govtproperties.setProperty("url","jdbc:mysql://db.cs.dal.ca:3306/harjot");
        Government contactTracer = new Government(govtproperties);
        Properties config = new Properties();
        config.setProperty("address", "127.192.1.1");
        config.setProperty("deviceName", "Oneplus 3T");
        String unique = config.getProperty("address") + config.getProperty("deviceName");
        //String deviceHash1 = HashGenerator(unique);
        String deviceHash1 = "Device-1";
        config.setProperty("deviceHash", deviceHash1);
        MobileDevice device1 = new MobileDevice(config, contactTracer);
        Properties config2 = new Properties();
        config2.setProperty("address", "117.192.1.1");
        config2.setProperty("deviceName", "Samsung Galaxy");
        unique = config2.getProperty("address") + config2.getProperty("deviceName");
    //    String deviceHash2 = HashGenerator(unique);
        String deviceHash2 = "Device-2";
        config2.setProperty("deviceHash", deviceHash2);
        MobileDevice device2 = new MobileDevice(config2,contactTracer);
        device1.recordContact(deviceHash2,31,2);
        device2.recordContact(deviceHash1,31,2);
        Properties config3 = new Properties();
        config3.setProperty("address", "217.292.11.1");
        config3.setProperty("deviceName", "iPhone 11 Pro");
        unique = config3.getProperty("address") + config3.getProperty("deviceName");
       // String deviceHash3 = HashGenerator(unique);
        String deviceHash3 = "Device-3";
        config3.setProperty("deviceHash", deviceHash3);
        MobileDevice device3 = new MobileDevice(config3,contactTracer);
        Properties config4 = new Properties();
        config4.setProperty("address", "317.232.21.11");
        config4.setProperty("deviceName", "iPhone 8 Pro");
        unique = config4.getProperty("address") + config4.getProperty("deviceName");
        //String deviceHash4 = HashGenerator(unique);
        String deviceHash4 = "Device-4";
        config4.setProperty("deviceHash", deviceHash4);
        MobileDevice device4 = new MobileDevice(config4,contactTracer);
        Properties config5 = new Properties();
        config5.setProperty("address", "412.432.211.211");
        config5.setProperty("deviceName", "iPhone 7 Pro");
        unique = config5.getProperty("address") + config5.getProperty("deviceName");
        //String deviceHash5 = HashGenerator(unique);
        String deviceHash5 = "Device-5";
        config5.setProperty("deviceHash", deviceHash5);
        MobileDevice device5 = new MobileDevice(config5,contactTracer);
        Properties config6 = new Properties();
        config6.setProperty("address", "112.42.22.61");
        config6.setProperty("deviceName", "Google Pixel 2");
        unique = config6.getProperty("address") + config6.getProperty("deviceName");
        //String deviceHash6 = HashGenerator(unique);
        String deviceHash6 = "Device-6";
        config6.setProperty("deviceHash", deviceHash6);
        MobileDevice device6 = new MobileDevice(config6,contactTracer);
        Properties config7 = new Properties();
        config7.setProperty("address", "12.242.122.711");
        config7.setProperty("deviceName", "Google Pixel 3");
        unique = config7.getProperty("address") + config7.getProperty("deviceName");
        //String deviceHash7 = HashGenerator(unique);
        String deviceHash7 = "Device-7";
        config7.setProperty("deviceHash", deviceHash7);
        MobileDevice device7 = new MobileDevice(config7,contactTracer);
        Properties config8 = new Properties();
        config8.setProperty("address", "42.51.172.111");
        config8.setProperty("deviceName", "Samsung Galaxy S11");
        unique = config8.getProperty("address") + config8.getProperty("deviceName");
       //String deviceHash8 = HashGenerator(unique);
        String deviceHash8 = "Device-8";
        config8.setProperty("deviceHash", deviceHash8);
        MobileDevice device8 = new MobileDevice(config8,contactTracer);
        Properties config9 = new Properties();
        config9.setProperty("address", "81.42.222.51");
        config9.setProperty("deviceName", "Samsung M3");
        unique = config9.getProperty("address") + config9.getProperty("deviceName");
        String deviceHash9 = HashGenerator(unique);
        config9.setProperty("deviceHash", deviceHash9);
        MobileDevice device9 = new MobileDevice(config9,contactTracer);
        Properties config10 = new Properties();
        config10.setProperty("address", "84.142.312.511");
        config10.setProperty("deviceName", "LG G5");
        unique = config10.getProperty("address") + config10.getProperty("deviceName");
        String deviceHash10 = HashGenerator(unique);
        config10.setProperty("deviceHash", deviceHash10);
        MobileDevice device10 = new MobileDevice(config10,contactTracer);
        device1.synchronizeData();
        device1.recordContact(deviceHash3,21,30);
        device3.recordContact(deviceHash1,21,30);
        device1.recordContact(deviceHash4,21,15);
        device4.recordContact(deviceHash1,21,15);
        device2.recordContact(deviceHash3,21,15);
        device3.recordContact(deviceHash2,21,15);
        device3.recordContact(deviceHash4,21,15);
        device4.recordContact(deviceHash3,21,15);
        device2.recordContact(deviceHash4,21,15);
        device4.recordContact(deviceHash2,21,15);
        device1.recordContact(deviceHash5,21,15);
        device5.recordContact(deviceHash1,21,15);
        device2.recordContact(deviceHash5,21,15);
        device5.recordContact(deviceHash2,21,15);
        device4.recordContact(deviceHash5,21,15);
        device5.recordContact(deviceHash4,21,15);
        device6.recordContact(deviceHash4,21,15);
        device4.recordContact(deviceHash6,21,15);
        device1.recordContact(deviceHash7,21,15);
        device7.recordContact(deviceHash1,21,15);
        device2.recordContact(deviceHash7,21,15);
        device7.recordContact(deviceHash2,21,15);
        device3.recordContact(deviceHash7,21,15);
        device7.recordContact(deviceHash3,21,15);
        device4.recordContact(deviceHash7,21,15);
        device7.recordContact(deviceHash4,21,15);
        device8.recordContact(deviceHash7,21,15);
        device7.recordContact(deviceHash8,21,15);
        device1.positiveTest(HashGenerator("2323AACCVVV"));
        device1.synchronizeData();
        System.out.println();
        System.out.println(device2.synchronizeData());
        System.out.println(device3.synchronizeData());
        System.out.println(device4.synchronizeData());
        System.out.println(device5.synchronizeData());
        System.out.println(device6.synchronizeData());
        System.out.println(device7.synchronizeData());
        System.out.println(device8.synchronizeData());



        contactTracer.recordTestResult(HashGenerator("2323AACCVVV"), 23, true);

        int a = contactTracer.findGatherings(21,1,2,1);

    }
}
