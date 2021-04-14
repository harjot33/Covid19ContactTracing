import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
        String deviceHash1 = HashGenerator(unique);
        config.setProperty("deviceHash", deviceHash1);
        MobileDevice device1 = new MobileDevice(config, contactTracer);
        Properties config2 = new Properties();
        config2.setProperty("address", "117.192.1.1");
        config2.setProperty("deviceName", "Samsung Galaxy");
        unique = config2.getProperty("address") + config2.getProperty("deviceName");
        String deviceHash2 = HashGenerator(unique);
        config2.setProperty("deviceHash", deviceHash2);
        MobileDevice device2 = new MobileDevice(config2,contactTracer);
        device1.recordContact(deviceHash2,31,2);
        device2.recordContact(deviceHash1,31,2);
        Properties config3 = new Properties();
        config3.setProperty("address", "217.292.11.1");
        config3.setProperty("deviceName", "iPhone 11 Pro");
        unique = config3.getProperty("address") + config3.getProperty("deviceName");
        String deviceHash3 = HashGenerator(unique);
        config3.setProperty("deviceHash", deviceHash3);
        MobileDevice device3 = new MobileDevice(config3,contactTracer);
        device1.recordContact(deviceHash3,21,30);
     device1.positiveTest(HashGenerator("2323AACCVVV"));
       device1.synchronizeData();
       // contactTracer.recordTestResult(HashGenerator("2323AACCVVV"), 23, true);

    }
}
