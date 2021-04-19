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


public class MobileDevice { // MobileDevice Class - This class is used to conceptualize a mobile device in which we perform tasks like recording contacts, positive test results and synchronizing with the government servers.
    Properties configFile; // Global Configuration File
    Government contactTracer; // Government Object Reference
    HashMap<Integer, Properties> contactList = new HashMap<>(); // contactList Hashmap which acts as a local contact list
    int contact_instance=0; // Instances of Contacts
    HashMap<String, ArrayList<String >> testrecord = new HashMap<>(); // This Hashmap stores the testrecords of a device.
    ArrayList<String> TestList = new ArrayList<>(); // This ArrayList is used to store the tests reported by a device.
    String initiator = ""; // Initializing initator with empty string

    MobileDevice(Properties configFile, Government contactTracer)  { // The constructor function is used to initialize the class variables with the parameters set during object creation.
            this.configFile = configFile;
            initiator = configFile.getProperty("deviceHash");
            this.contactTracer = contactTracer;

    }


    public void recordContact(String individual, int date, int duration){ // This method is used to record the mobile devices which have come in contact with the host device, it records the date and the duration of the encounter.

        if(individual==null || individual.isEmpty() || date<0 || duration <=0){ // Input Validations - These are used to weed out the unwanted illegal data.
            return;

        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy"); // Converting the days into the proper date format.
        Calendar calendar = Calendar.getInstance(); // Getting the instance of the Calendar.
        try {
            calendar.setTime(sdf.parse("01-01-2021")); // Parsing the date into the Simple Date Format
        }catch (ParseException pe){ // Incase the parse exception arises, this catch statement with deal with it
            System.out.println(pe); // Printing the exception onto the console screen.
        }

        calendar.add(Calendar.DAY_OF_MONTH,date); // Adding the days to the pre-set date.
        String dateofcontact = sdf.format(calendar.getTime()); // Getting the string of the date, ( It is previously in the Date format )
        Properties p = new Properties(); // A new properties object 'p' signifying the properties of the mobile device.
        p.setProperty("deviceHash",individual); // We set the properties of the device
        p.setProperty("date",dateofcontact);
        p.setProperty("duration",""+duration);
        contactList.put(contact_instance,p);// Here we add the contact instance to the local contact list, along with the properties of the device.
        contact_instance ++; // We increment the contact instance.

    }

    public void positiveTest(String testHash) { // This method is used by the user to record presence a positive test locally, this signifies that the user has tested positive for COVID-19.
        if(testHash==null || testHash.isEmpty()){ // Input Validation to weed out the illegal data
            System.out.println("Invalid Test Hash Entered.");
            return; // Return from the function.
        }
        String deviceHash = configFile.getProperty("deviceHash"); // Retrieving the deviceHash property and setting it as deviceHash.
        if (!TestList.contains(testHash)) { // Making sure no duplicate test hashes are entered into the testlist
            TestList.add(testHash);
            testrecord.put(deviceHash, TestList); // Adding the TestList of a particular device to the testrecord.
        } else {
            System.out.println("Test has already been reported."); // We report back that no duplicate tests will be inserted
        }

    }

    public boolean synchronizeData() throws Exception { // This function is most vital function in this program as it is used to synchronize the local data with the database of the government.
        // This function reports back to the user if they have been in contact with a person who has tested positive in the last 14 days.
        // I have used the absolute value of the date the test was taken - +-14 Days from the Test Date will be used to determine a positive contact.
        DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance(); // Using the DOM DocumentBuilderFactory's instance to create a documentfactory object
        DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder(); // We create a new documentBuilder
        Document document = documentBuilder.newDocument(); // Creating document as a new instance of the documentBuilder.
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
        for(int i = 0 ; i<contactList.size();i++){ // Iterating over the contactList which stores the contacts of the device
            Element device = document.createElement("device"); // Creating an element "device"  using the createElement method.
            device.appendChild(document.createTextNode(i+""));
            contacts_list.appendChild(device); // contacts_list parent adds device as its child

            Element deviceHash = document.createElement("deviceHash"); // Creating an element "deviceHash" using the createElement method.
            deviceHash.appendChild(document.createTextNode(contactList.get(i).getProperty("deviceHash"))); // Adding the TextNode as child which contains the name of the device
            device.appendChild(deviceHash);// device parent adds deviceHash as its child

            Element date = document.createElement("date"); // Creating an element "date" using the createElement method.
            date.appendChild(document.createTextNode(contactList.get(i).getProperty("date"))); // Adding the TextNode as child which contains the date of contact wth the device
            device.appendChild(date); // device parent adds date as its child

            Element duration = document.createElement("duration"); // Creating an element "duration" using the createElement method.
            duration.appendChild(document.createTextNode(contactList.get(i).getProperty("duration")));  // Adding the TextNode as child which contains the duration of contact wth the device
            device.appendChild(duration); // device parent adds duration as its child
        }


        Transformer transformer = TransformerFactory.newInstance().newTransformer(); // Initializing Transformer Object with an instance of TransformerFactory
        StringWriter writer = new StringWriter(); // New Instance "writer" of the StringWriter Class
        transformer.transform(new DOMSource(document), new StreamResult(writer)); // We transform using an instance of DomSource with document has its paraemeter, along with instance of SteamResult with writer as parameter.
        String contactInfo = writer.getBuffer().toString(); // Creating the string contact info which contains all the information such as the contact list, date, duration.
        return contactTracer.mobileContact(initiator,contactInfo); // Call the Government class's method mobileContact using the contactTracer object which as initatior (host device) and its information string as the parameters.
     }

    public static String HashGenerator(String deviceName) throws NoSuchAlgorithmException { // This is a static function which is used to generate the SHA - 256 Hashes of the Device and its Address.

        MessageDigest md = MessageDigest.getInstance("SHA-256"); // Instantiate MessageDigest Class with "SHA-256" as the format.
        byte[] sha256 = md.digest(deviceName.getBytes(StandardCharsets.UTF_8)); // We take the string and convert it into UTF_8 format Bye stream
        BigInteger sig = new BigInteger(1, sha256); // Convert it into the Signum Reperesentation.
        String deviceHash = sig.toString(16); // Then we further convert it into the string.
        return deviceHash; // Return the resulting hash to the calling function.
    }




    public static void main(String[] args) throws Exception { // Main Calling Function - This function drives the program flow.
        // Here we define the mobile devices and their properties.
        Properties govtproperties = new Properties(); // We create a properties object to define the properties of the Government.
        govtproperties.setProperty("user","harjot"); // Used to store the username
        govtproperties.setProperty("password","B00872298"); // Used to store the password
        govtproperties.setProperty("url","jdbc:mysql://db.cs.dal.ca:3306/harjot"); //// Used to store the Database URL
        Government contactTracer = new Government(govtproperties); // The Government Class Object is created with govtproperties as the parameter for the constructor
        Properties config = new Properties(); // We create the config of the first mobile device
        config.setProperty("address", "127.192.1.1"); // Defining the address of the mobile device
        config.setProperty("deviceName", "Oneplus 3T"); // Defining the name of the mobile device
        String unique = config.getProperty("address") + config.getProperty("deviceName"); // We use string concatenation to combine both device name and the address.
        String deviceHash1 = HashGenerator(unique); // Send the combined string as parameter to generate the unique Hash of the device.
        config.setProperty("deviceHash", deviceHash1);// We create the config of the second mobile device
        MobileDevice device1 = new MobileDevice(config, contactTracer); // Create the instance of the first mobile device with its config properties and the government object.
        Properties config2 = new Properties(); // We create the config properties of the second mobile device
        config2.setProperty("address", "117.192.1.1"); // Defining the address of the mobile device
        config2.setProperty("deviceName", "Samsung Galaxy");  // Defining the name of the mobile device
        unique = config2.getProperty("address") + config2.getProperty("deviceName"); // We use string concatenation to combine both device name and the address.
     String deviceHash2 = HashGenerator(unique); // Send the combined string as parameter to generate the unique Hash of the device.
        config2.setProperty("deviceHash", deviceHash2); // Setting the Hash of the device.
        MobileDevice device2 = new MobileDevice(config2,contactTracer); // Creating the instance of the device - 2 with config properties and contactTracer object.
        device1.recordContact(deviceHash2,50,30); // Recording the contact of device 1 with device 2.
        device2.recordContact(deviceHash1,50,30); // Recordng the contact of device 2 with device 1.
        Properties config3 = new Properties(); // We create the config of the third mobile device
        config3.setProperty("address", "217.292.11.1");  // Defining the address of the mobile device
        config3.setProperty("deviceName", "iPhone 11 Pro"); // Defining the name of the mobile device
        unique = config3.getProperty("address") + config3.getProperty("deviceName"); // We use string concatenation to combine both device name and the address.
        String deviceHash3 = HashGenerator(unique); // Send the combined string as parameter to generate the unique Hash of the device.
        config3.setProperty("deviceHash", deviceHash3);  // Setting the Hash of the device.
        MobileDevice device3 = new MobileDevice(config3,contactTracer); // Creating the instance of the device third with config properties and contactTracer object.
        Properties config4 = new Properties();  // We create the config of the fourth mobile device
        config4.setProperty("address", "317.232.21.11"); // Defining the address of the mobile device
        config4.setProperty("deviceName", "iPhone 8 Pro"); // Defining the name of the mobile device
        unique = config4.getProperty("address") + config4.getProperty("deviceName"); // We use string concatenation to combine both device name and the address.
        String deviceHash4 = HashGenerator(unique); // Send the combined string as parameter to generate the unique Hash of the device.
        config4.setProperty("deviceHash", deviceHash4); //Setting the Hash
        MobileDevice device4 = new MobileDevice(config4,contactTracer); // Object Creation of the device
        Properties config5 = new Properties(); // New Mobile Device Properties
        config5.setProperty("address", "412.432.211.211"); // Setting the address of the device
        config5.setProperty("deviceName", "iPhone 7 Pro"); // Setting the name of the device
        unique = config5.getProperty("address") + config5.getProperty("deviceName"); // We use string concatenation to combine both device name and the address.
        String deviceHash5 = HashGenerator(unique);// Send the combined string as parameter to generate the unique Hash of the device.
        config5.setProperty("deviceHash", deviceHash5);//Setting the Hash
        MobileDevice device5 = new MobileDevice(config5,contactTracer); // Object Creation of the device
        Properties config6 = new Properties(); // New Mobile Device Properties
        config6.setProperty("address", "112.42.22.61"); // Setting the address of the device
        config6.setProperty("deviceName", "Google Pixel 2");// Setting the name of the device
        unique = config6.getProperty("address") + config6.getProperty("deviceName");// We use string concatenation to combine both device name and the address.
        String deviceHash6 = HashGenerator(unique);// Send the combined string as parameter to generate the unique Hash of the device.
        config6.setProperty("deviceHash", deviceHash6); //Setting the Hash
        MobileDevice device6 = new MobileDevice(config6,contactTracer); // Object Creation of the device
        Properties config7 = new Properties(); // New Mobile Device Properties
        config7.setProperty("address", "12.242.122.711");  // Setting the address of the device
        config7.setProperty("deviceName", "Google Pixel 3"); // Setting the name of the device
        unique = config7.getProperty("address") + config7.getProperty("deviceName"); // We use string concatenation to combine both device name and the address.
        String deviceHash7 = HashGenerator(unique); // Send the combined string as parameter to generate the unique Hash of the device.
        config7.setProperty("deviceHash", deviceHash7);  //Setting the Hash
        MobileDevice device7 = new MobileDevice(config7,contactTracer);  // Object Creation of the device
        Properties config8 = new Properties(); // New Mobile Device Properties
        config8.setProperty("address", "42.51.172.111"); // Setting the address of the device
        config8.setProperty("deviceName", "Samsung Galaxy S11");  // Setting the name of the device
        unique = config8.getProperty("address") + config8.getProperty("deviceName"); // We use string concatenation to combine both device name and the address.
       String deviceHash8 = HashGenerator(unique); // Send the combined string as parameter to generate the unique Hash of the device.
        config8.setProperty("deviceHash", deviceHash8); //Setting the Hash
        MobileDevice device8 = new MobileDevice(config8,contactTracer); // Object Creation of the device
        Properties config9 = new Properties(); // New Mobile Device Properties
        config9.setProperty("address", "81.42.222.51"); // Setting the address of the device
        config9.setProperty("deviceName", "Samsung M3");  // Setting the name of the device
        unique = config9.getProperty("address") + config9.getProperty("deviceName"); // We use string concatenation to combine both device name and the address.
        String deviceHash9 = HashGenerator(unique); // Send the combined string as parameter to generate the unique Hash of the device.
        config9.setProperty("deviceHash", deviceHash9); //Setting the Hash
        MobileDevice device9 = new MobileDevice(config9,contactTracer);// Object Creation of the device
        Properties config10 = new Properties(); // New Mobile Device Properties
        config10.setProperty("address", "84.142.312.511"); // Setting the address of the device
        config10.setProperty("deviceName", "LG G5");  // Setting the name of the device
        unique = config10.getProperty("address") + config10.getProperty("deviceName"); // We use string concatenation to combine both device name and the address.
        String deviceHash10 = HashGenerator(unique); // Send the combined string as parameter to generate the unique Hash of the device.
        config10.setProperty("deviceHash", deviceHash10); // Setting the deviceHash of the device
        MobileDevice device10 = new MobileDevice(config10,contactTracer); // Object Creation of the devic
        device1.synchronizeData(); // Synching device 1.

        // The below statements are used to record contacts between the devices.

        device1.recordContact(deviceHash3,50,30);
        device3.recordContact(deviceHash1,50,30);
        device1.recordContact(deviceHash3,50,35);
        device3.recordContact(deviceHash1,50,35);
        device1.recordContact(deviceHash3,50,22);
        device3.recordContact(deviceHash1,50,22);
        device1.recordContact(deviceHash4,50,37);
        device4.recordContact(deviceHash1,50,37);
        device2.recordContact(deviceHash3,50,38);
        device3.recordContact(deviceHash2,50,38);
        device3.recordContact(deviceHash4,50,36);
        device4.recordContact(deviceHash3,50,36);
        device2.recordContact(deviceHash4,50,41);
        device4.recordContact(deviceHash2,50,41);
        device1.recordContact(deviceHash5,21,15);
        device5.recordContact(deviceHash1,21,15);
        device2.recordContact(deviceHash5,50,39);
        device5.recordContact(deviceHash2,50,39);
        device4.recordContact(deviceHash5,50,34);
        device5.recordContact(deviceHash4,50,34);
        device6.recordContact(deviceHash4,21,15);
        device5.recordContact(deviceHash6,50,42);
        device6.recordContact(deviceHash5,50,42);
        device5.recordContact(deviceHash3,50,41);
        device3.recordContact(deviceHash5,50,41);
        device4.recordContact(deviceHash6,50,47);
        device6.recordContact(deviceHash4,50,34);
        device4.recordContact(deviceHash6,50,34);
        device1.recordContact(deviceHash7,21,15);
        device7.recordContact(deviceHash1,21,15);
        device2.recordContact(deviceHash7,50,28);
        device7.recordContact(deviceHash2,50,28);
        device3.recordContact(deviceHash7,50,47);
        device7.recordContact(deviceHash3,50,47);
        device4.recordContact(deviceHash7,21,15);
        device7.recordContact(deviceHash4,21,15);
        device8.recordContact(deviceHash7,21,15);
        device7.recordContact(deviceHash8,21,15);
        device1.positiveTest(HashGenerator("2323AACCVVV")); // This method call is used by the user to record a positive test
        // The following statements are used to synchronize various devices with the SynchronizeData Method.
       //contactTracer.recordTestResult(HashGenerator("2323AACCVVV"), 50, true); // This method call is used to record testresult into the government's database
        System.out.println("DEVICE - 1 "+device1.synchronizeData());
        System.out.println("DEVICE - 2 "+device2.synchronizeData());
        System.out.println("DEVICE - 3 "+device3.synchronizeData());
        System.out.println("DEVICE - 4 "+device4.synchronizeData());
        System.out.println("DEVICE - 5 "+device5.synchronizeData());
        System.out.println("DEVICE - 6 "+device6.synchronizeData());
        System.out.println("DEVICE - 7 "+device7.synchronizeData());
        System.out.println("DEVICE - 8 "+device8.synchronizeData());


        System.out.println(contactTracer.findGatherings(50, 2, 32, 0.6f));
        System.out.println(contactTracer.findGatherings(50, 3, 32, 0.6f));
        System.out.println(contactTracer.findGatherings(50, 4, 32, 0.49f));
        System.out.println(contactTracer.findGatherings(50, 5, 32, 0.49f));

    }
}
