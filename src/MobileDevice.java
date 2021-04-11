import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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
    static int M_ID = 1;
    int contact_instance=0;
    int synchchecker=0;
    HashMap<String, ArrayList<String >> testrecord = new HashMap<>();
    ArrayList<String> TestList = new ArrayList<>();

    MobileDevice(Properties configFile, Government contactTracer)  {
            this.configFile = configFile;
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
        contact_instance ++;
        contactList.put(contact_instance,p);
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

    public boolean synchronizeData() throws ParserConfigurationException {
        DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance(); // Using the DOM DocumentBuilderFactory's instance to create a documentfactory object
        DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder(); // We create a new documentBuilder
        Document document = documentBuilder.newDocument();
        Element information = document.createElement("information"); //We create the root element of the document tree
        document.appendChild(information); // We append it to the document tree as the root child

        Element device_hash = document.createElement("device_hash"); //We create the root element of the document tree
        device_hash.appendChild(document.createTextNode(configFile.getProperty("deviceHash"))); // We set the child of this node as a textnode, depicting some text value
        information.appendChild(device_hash); // We append it to the document tree as the root child

        Element contacts_list = document.createElement("contact_list"); //We create the root element of the document tree
        for(int i = synchchecker ; i<contactList.size();i++){
            contacts_list.appendChild(document.createTextNode(configFile.getProperty("deviceHash"))); // We set the child of this node as a textnode, depicting some text value
        }
        information.appendChild(contacts_list); // We append it to the document tree as the root child


        return true;
    }

    private static String HashGenerator(String deviceName) throws NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] sha256 = md.digest(deviceName.getBytes(StandardCharsets.UTF_8));
        BigInteger sig = new BigInteger(1, sha256);
        String deviceHash = sig.toString(16);
        System.out.println(deviceHash);
        return deviceHash;
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        Government contactTracer = new Government();
        Properties config = new Properties();
        config.setProperty("address", "127.192.1.1");
        config.setProperty("deviceName", "Oneplus 3T");
        Random ID_No = new Random();
        int Mobile_ID = ID_No.nextInt(9000000) + 1000000;
        String unique = config.getProperty("address") + config.getProperty("deviceName") + Mobile_ID;
        String deviceHash1 = HashGenerator(unique);
        config.setProperty("deviceHash", deviceHash1);
        MobileDevice device1 = new MobileDevice(config, contactTracer);
        Properties config2 = new Properties();
        config2.setProperty("address", "117.192.1.1");
        config2.setProperty("deviceName", "Samsung Galaxy");
        Mobile_ID = ID_No.nextInt(9000000) + 1000000;
        unique = config2.getProperty("address") + config2.getProperty("deviceName") + Mobile_ID;
        String deviceHash2 = HashGenerator(unique);
        config2.setProperty("deviceHash", deviceHash2);
        MobileDevice device2 = new MobileDevice(config2,contactTracer);
        device1.recordContact(deviceHash2,31,2);
        device2.recordContact(deviceHash1,31,2);
        device1.recordContact("3333",31,2);

    }
}
