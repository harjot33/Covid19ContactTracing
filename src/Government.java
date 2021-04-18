// Package Importing
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class Government { // This Government Class is used to mimic how the Government works in the real world, here we maintain the databases, the users connect to the database frequently and they upload their data.
    // The user gets to know if they have recently come into contact with a positive person.
    Properties configFile; // The config file has been created as a reference.
    ResultSet resultSet = null; // Initializing resultSet with null
    Statement statement = null; // Initializing statement with null
    Statement rstatement = null; // Initializing rstatement with null
    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy"); // Simple Date Format with the dd-MM-yyyy format

    Government(Properties configFile){ // The constructor function of the class, used to initialize the configFile variable.
            this.configFile = configFile;
    }

    boolean mobileContact(String initiator, String contactInfo) throws SQLException, ParserConfigurationException, IOException, SAXException, ParseException {
        // This method is used to connect to the database and then make queries to it, new devices can use this method to upload data to the database, existing ones can upload new contacts onto the database
        // The users will be informed if they have been in contact with any positive person in the last 14 days.
        // This method also is used to validate the tests and to set the relevant information into the database.
        ConnectionEshtablisher(initiator);// We make use of the ConnectionEstablisher method to establish a connection to the database.
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Creating a new instance of DocmentBuilderFactory
        DocumentBuilder builder = factory.newDocumentBuilder(); // builder is created as a new instance
        Document document = builder.parse( new InputSource( new StringReader(contactInfo))); // A new Document is created with the help of the contactinfo string
        boolean notest = false; // This signifies that there is no test done by the host device
        String sourceHash = document.getElementsByTagName("sourceHash").item(0).getTextContent(); // Getting the hash of the host device.
        boolean comecontact = false; // This is set to be true if a device has come in contact with a covid posiitve person.
        ResultSet rs1 = null; // A variable of the ResultSet class.
        if (!resultSet.isBeforeFirst() ) { // If there are no entries with the device hash of the host device, means that the database doesn't have any entries of the host device.
            NodeList testlist = document.getElementsByTagName("testHash"); // We get the testHash
            String testhash=""; // Initializing testhash with an empty string.
            ArrayList<String> contactlist = new ArrayList<>();  // ArrayList contactlist for storing the contacts of the host device.
            ArrayList<String> dateList = new ArrayList<>(); // Arraylist datelist for storing the date of the contact
            ArrayList<String> durationList = new ArrayList<>(); // Arraylist durationlist for storing the duration of the contact
            boolean singulartest = false; // Tells us if there is only one test.
            String positive_contact =""; // Initializing positive_contact with an empty string.
            String pcontact_date = ""; // Initializing pcontact_date with an empty string.
            String positive_status = "";  // Initializing positive_status with an empty string.
            String ptest_date = "";  // Initializing ptest_date with an empty string.
            String exception_date = ""; // Initializing exception_date with an empty string.
            String pctest_date="";  // Initializing pctest_date with an empty string.
            String ftcdate=""; // Initializing ftcdate with an empty string.
            String ptcdate=""; // Initializing ptcdate with an empty string.
            String last_contact = ""; // Initializing last_contact with an empty string.
            String usernotified=""; // Initializing usernotified with an empty string.

            Calendar calendar = Calendar.getInstance(); //Calendar Instance


            resultSet = statement.executeQuery("Select source_device,contact_date,contact_duration from contact_tracker where contact_device='"+sourceHash+"'");
            // Query to extract the information of the device to which the host device has come in contact with
            if(resultSet.isBeforeFirst()){ // If the resultset is not empty
                while (resultSet.next()){

                    String deviceHash = resultSet.getString("source_device"); // Getting the hash of the said device
                    String contact_date = resultSet.getString("contact_date"); // Date of contact
                    String contact_duration = resultSet.getString("contact_duration"); // Duration of contact
                    rs1 = rstatement.executeQuery("Select * from devicerecord where deviceHash='"+deviceHash+"';"); // Getting the device details of the device with whom source device came in contact
                    rs1.next(); // Shifting the cursor to the data
                    String cp_status = rs1.getString("positive_status"); // Getting the status of the device
                    if(cp_status.equals("true")){ // If it is a positive status device
                        positive_contact = rs1.getString("deviceHash");
                        pcontact_date = contact_date;
                        pctest_date =  rs1.getString("ptest_date");
                        ftcdate = daysAddition(pctest_date,14); // We check if the date of contact is within the 14 days
                        ptcdate = daysAddition(pctest_date,-14); // We check if the date of contact is within the 14 days

                        if(WithinPositveRange(pcontact_date,ptcdate,ftcdate)){ // If contact date indeed is +-14 days since the test date
                            positive_status = "false";
                            ptest_date = "null";
                            comecontact = true; // Setting the comecontact as true.
                        }
                    }
                }
            }


            if(!document.getElementsByTagName("testHash").item(0).getTextContent().isEmpty()){ // Checking if the device has no testhashes, if it is empty.
                ArrayList<String> thashlist = new ArrayList<>(); // Initializing Arraylist thashlist

                for(int i = 0 ; i<testlist.getLength(); i++){ // Looping through the testlist arraylist
                    testhash = document.getElementsByTagName("testHash").item(i).getTextContent(); // Getting the testhash of one of tests present in the last
                    //    System.out.println(testhash);
                    resultSet = statement.executeQuery("Select TestHash,testDate, TestResult from TestRecord where TestHash ='"+testhash+"';"); // Getting complete details based on the testhash

                    if(resultSet.isBeforeFirst()) { // If the resultSet is not empty
                        resultSet.next(); // Move the cursor of the resultSet
                        String govtresult = resultSet.getString("TestResult");
                        if(govtresult.equals("true")) // If the testresult matches with the result of the same test in DB
                            statement.executeUpdate("Update TestResult SET TestDevice='"+sourceHash+"' where TestHash='"+testhash+"';"); // Update the device in the DB with its test.
                        thashlist.add(resultSet.getString("TestHash")); // Add the testhash to the thaslist
                    }
                }

                if(thashlist.size()==1 && thashlist.get(0).equals(document.getElementsByTagName("testHash").item(0).getTextContent())){ // If the device has only 1 test in its testlist record.
                    String govtresult = resultSet.getString("TestResult"); // We get the result of the test
                    if(govtresult.equals("true")){
                        System.out.println("TEST HAS BEEN VALIDATED"); // If the results match, then we have validated
                        singulartest  = true; // Set it as true to tell there is only one test.
                    }

                }else if(thashlist.size()>1){ // If the testlist has more than 1 tests asssociated with a single host device
                    Map<Integer, String> testdateregister = new HashMap<>();
                    ArrayList<Integer> keytracker = new ArrayList<>();
                    ArrayList<String> ExceptionDate = new ArrayList<>();

                    for(int i =0 ; i<thashlist.size();i++){
                        testhash = thashlist.get(0);
                        resultSet = statement.executeQuery("Select TestHash,testDate, TestResult from TestRecord where TestHash ='"+testhash+"';"); // Query to get details about a test.
                        String govtresult = resultSet.getString("TestResult");
                        if(govtresult.equals("true")){ // Validating test result with local and DB
                            System.out.println("TEST HAS BEEN VALIDATED");
                            String date = resultSet.getString("testDate"); // Getting the date of the test
                            testdateregister.put(i,date); //Adding it to the test data register
                            keytracker.add(i); // Keeping track of the keys.
                        }
                    }
                    String smalldate = testdateregister.get(0); // Getting the date of the first test.
                    Date firstdate = new Date();
                    Date date1 = sdf.parse(smalldate);
                    for(int i =1 ; i<testdateregister.size();i++){ // We are using this loop to check if there are any exceptional tests.
                        // Exceptional Tests mean that these are the tests that have occurred 14 days after testing positive, they are rare occurences.
                        Date date2 = sdf.parse(testdateregister.get(i));
                        if(date2.after(date1)){
                            // In these conditional statements we check if the date of the one test is before the next one, if we find that there is a gap of 14 days between two tests, then we add it to the exception list.
                            firstdate = date1;
                            calendar.setTime(date1);
                            calendar.add(Calendar.DAY_OF_MONTH,14);
                            String futuredate = sdf.format(calendar.getTime());
                            Date negativedate = sdf.parse(futuredate);
                            String entry = sdf.format(negativedate);

                            if(negativedate.before(date2)){
                                ExceptionDate.add(entry);
                            }

                        }else{
                            firstdate = date2;
                            calendar.setTime(firstdate);
                            calendar.add(Calendar.DAY_OF_MONTH,14);
                            String futuredate = sdf.format(calendar.getTime());
                            Date negativedate = sdf.parse(futuredate);
                            String entry = sdf.format(negativedate);

                            if(negativedate.before(date1)){
                                ExceptionDate.add(entry);
                            }
                        }
                    }
                    Date final_date = sdf.parse("01-01-2021");
                    if(ExceptionDate.size()!=0){ // If we find that there are exceptional tests, then we iteratve over them and then set the positive_date of the device as that of the exceptional date.
                        for(int i =0 ; i<ExceptionDate.size();i++){
                            Date current_date = sdf.parse(ExceptionDate.get(i));

                            if(current_date.after(final_date)){
                                final_date = current_date;
                            }
                        }
                        exception_date = sdf.format(final_date);
                        pcontact_date = "null";
                        positive_contact = "SELF";
                        ptest_date = sdf.format(firstdate);
                        positive_status="true";
                    }else{
                        pcontact_date = "null";
                        positive_contact = "SELF";
                        ptest_date = sdf.format(firstdate);
                        positive_status="true";
                    }


                }
            }else{
                notest = true; // If there are no tests related to the source device.
            }

            NodeList ok = document.getElementsByTagName("device"); // Get the elements related to the device.

         //   System.out.println(document.getElementsByTagName("testHash").item(0).getTextContent());
            for(int i =0 ; i < ok.getLength(); i++){ // In this iterative statement, we loop over the elements as we add to the contact, date and duration arraylists.
                        String deviceHash  =document.getElementsByTagName("deviceHash").item(i).getTextContent();
                        contactlist.add(deviceHash);
                        String date = document.getElementsByTagName("date").item(i).getTextContent();
                        dateList.add(date);
                        String duration = document.getElementsByTagName("duration").item(i).getTextContent();
                        durationList.add(duration);
                        System.out.println(deviceHash+"-"+date+"-"+duration+"-"+positive_status);
                    }



            // statement.executeUpdate("INSERT INTO devicerecord (devicehash, contact_list, last_contact)\n" +"Values('" + sourceHash + "','" + contactlistHash + "','"+result+"')");
            if(contactlist.size()>0){ // If there are more than 0 contacts in the contact list.
                 last_contact = contactlist.get(contactlist.size()-1); // We set the last contact by referring to the last contact of the list

            }
            if(singulartest && !comecontact){ // If there is a single test and the user has not come in contact with anyone who has COVID.
              //  positive_contact = resultSet.getString("")
                ptest_date = resultSet.getString("testDate");
                positive_status = resultSet.getString("TestResult");
                positive_contact = "SELF";
                pcontact_date = "null";
                usernotified = "false";
                String contactstring = String.join(",", contactlist);
                statement.executeUpdate("INSERT INTO devicerecord (devicehash, contact_list, last_contact,positive_contact, pcontact_date, positive_status, ptest_date, usernotified)" +"Values('" + sourceHash + "','" + contactstring + "','"+last_contact+"','"+positive_contact+"','"+pcontact_date+"','"+positive_status+"','"+ptest_date+"','"+usernotified+"')");
                for(int i =0 ; i<contactlist.size();i++){
                    statement.executeUpdate("INSERT INTO contact_tracker (source_device, contact_device, contact_date,contact_duration)"+ "Values('" + sourceHash + "','" + contactlist.get(i) + "','"+dateList.get(i)+"','"+durationList.get(i)+"')");

                }

            }else if(notest && !comecontact){
                ptest_date = "null";
                positive_status = "false";
                positive_contact = "null";
                pcontact_date = "null";
                usernotified = "false";
                String contactstring = String.join(",", contactlist);
                statement.executeUpdate("INSERT INTO devicerecord (devicehash, contact_list, last_contact,positive_contact, pcontact_date, positive_status, ptest_date,usernotified)" +"Values('" + sourceHash + "','" + contactstring + "','"+last_contact+"','"+positive_contact+"','"+pcontact_date+"','"+positive_status+"','"+ptest_date+"','"+usernotified+"')");
                for(int i =0 ; i<contactlist.size();i++){
                    statement.executeUpdate("INSERT INTO contact_tracker (source_device, contact_device, contact_date,contact_duration)"+ "Values('" + sourceHash + "','" + contactlist.get(i) + "','"+dateList.get(i)+"','"+durationList.get(i)+"')");

                }

            }else if(!comecontact){
                String contactstring = String.join(",", contactlist);
                usernotified = "false";
                statement.executeUpdate("INSERT INTO devicerecord (devicehash, contact_list, last_contact,positive_contact, pcontact_date, positive_status, ptest_date,usernotified)" + "Values('" + sourceHash + "','" + contactstring + "','" + last_contact + "','" + positive_contact + "','" + pcontact_date + "','" + positive_status + "','" + ptest_date+"','"+usernotified + "')");
                for(int i =0 ; i<contactlist.size();i++){
                    statement.executeUpdate("INSERT INTO contact_tracker (source_device, contact_device, contact_date,contact_duration)"+ "Values('" + sourceHash + "','" + contactlist.get(i) + "','"+dateList.get(i)+"','"+durationList.get(i)+"')");
                }

            }else if(comecontact){
                String contactstring = String.join(",", contactlist);
                usernotified = "true";
                rstatement.executeUpdate("Update devicerecord SET usernotified='true' where devicehash='"+sourceHash+"';");
                statement.executeUpdate("INSERT INTO devicerecord (devicehash, contact_list, last_contact,positive_contact, pcontact_date, positive_status, ptest_date, usernotified)" +"Values('" + sourceHash + "','" + contactstring + "','"+last_contact+"','"+positive_contact+"','"+pcontact_date+"','"+positive_status+"','"+ptest_date+"','"+usernotified+"')");
                for(int i =0 ; i<contactlist.size();i++){
                    statement.executeUpdate("INSERT INTO contact_tracker (source_device, contact_device, contact_date,contact_duration)"+ "Values('" + sourceHash + "','" + contactlist.get(i) + "','"+dateList.get(i)+"','"+durationList.get(i)+"')");
                }


            }


        }else {
            boolean notif = false;
            NodeList ok = document.getElementsByTagName("device");
            ArrayList<String> contactlist = new ArrayList<>();
            ArrayList<String> datelist = new ArrayList<>();
            ArrayList<String> durationlist = new ArrayList<>();
            String duplicateverification;
            String lastcontactDB;
            String lastcontactLO;
            String usernotified;
            String positive_status = "";
            String positive_contact ;
            String pcontact_date;
            String pctest_date;
            String ftcdate ;
            String ptcdate;
            String ptest_date;
            resultSet.next();
            for(int i =0 ; i < ok.getLength(); i++){
                String deviceHash  =document.getElementsByTagName("deviceHash").item(i).getTextContent();
                contactlist.add(deviceHash);
                String date = document.getElementsByTagName("date").item(i).getTextContent();
                datelist.add(date);
                String duration = document.getElementsByTagName("duration").item(i).getTextContent();
                durationlist.add(duration);
            }

            duplicateverification = resultSet.getString("contact_list");
            lastcontactDB = resultSet.getString("last_contact");
            positive_status = resultSet.getString("positive_status");
            lastcontactLO = contactlist.get(contactlist.size()-1);
            String[] devices = duplicateverification.split(",");
            List<String> devicelist = new ArrayList<String>(Arrays.asList(devices));
            usernotified = resultSet.getString("usernotified");
            String newclist = duplicateverification;
            String lastcontact="";


            if(devices.length == contactlist.size() && lastcontactDB.equals(lastcontactLO)){
                if(usernotified.equals("true")){
                    notif = true;
                }
                System.out.println("No new contacts.");
            }else {
                int beg = contactlist.indexOf(lastcontactDB);
                beg++;
                for (int i = beg; i < contactlist.size(); i++) {
                    String incominghash = contactlist.get(i);

                    if(devicelist.contains(incominghash)){
                        resultSet  = statement.executeQuery("Select source_device,contact_date,contact_duration from contact_tracker where contact_device='"+incominghash+"'");
                        resultSet.next();
                        String cdate = resultSet.getString("contact_date");
                        if(cdate.equals(datelist.get(i))){
                            String duration = resultSet.getString("contact_duration");
                            int dur = Integer.parseInt(duration);
                            String ldur = durationlist.get(i);
                            int dur1 = Integer.parseInt(ldur);
                            dur = dur+dur1;
                            String finaldur = dur+"";
                            rstatement.executeUpdate("Update contact_tracker SET contact_duration='"+finaldur+"' where contact_duration='"+duration+"' AND source_device='"+sourceHash+"';");
                        }

                    }else{
                        if(i==contactlist.size()-1){
                             lastcontact = incominghash;
                        }
                        newclist=newclist+","+incominghash;
                        rstatement.executeUpdate("INSERT INTO contact_tracker (source_device, contact_device, contact_date,contact_duration)"+ "Values('" + sourceHash + "','" + contactlist.get(i) + "','"+datelist.get(i)+"','"+durationlist.get(i)+"')");

                    }
                    String contact_date = "";
                    rs1 = rstatement.executeQuery("Select * from devicerecord where deviceHash='"+incominghash+"';");
                    resultSet = statement.executeQuery("Select * from contact_tracker where source_device='"+sourceHash+"' AND contact_device='"+incominghash+"';");
                    if(resultSet.isBeforeFirst()){
                        resultSet.next();
                        contact_date = resultSet.getString("contact_date");
                    }
                    rs1.next();
                    String cp_status = rs1.getString("positive_status");
                    if(cp_status.equals("true")){
                        positive_contact = rs1.getString("deviceHash");
                        pcontact_date = contact_date;
                        pctest_date =  rs1.getString("ptest_date");
                        ftcdate = daysAddition(pctest_date,14);
                        ptcdate = daysAddition(pctest_date,-14);

                        if(WithinPositveRange(pcontact_date,ptcdate,ftcdate)){
                            positive_status = "false";
                            ptest_date = "null";
                            comecontact = true;
                            usernotified="true";
                            rstatement.executeUpdate("Update devicerecord SET usernotified='true' where devicehash='"+sourceHash+"';");
                        }

                    }

                }
                rstatement.executeUpdate("Update devicerecord SET contact_list='"+newclist+"' where devicehash='"+sourceHash+"';");
                rstatement.executeUpdate("Update devicerecord SET last_contact='"+lastcontact+"' where devicehash='"+sourceHash+"';");


            }

            if(usernotified.equals("false") && positive_status.equals("false")){
                resultSet.close();
                resultSet = statement.executeQuery("Select source_device,contact_date,contact_duration from contact_tracker where contact_device='"+sourceHash+"'");
                    if(resultSet.isBeforeFirst()){
                        while (resultSet.next()){
                            String deviceHash = resultSet.getString("source_device");
                            String contact_date = resultSet.getString("contact_date");
                            String contact_duration = resultSet.getString("contact_duration");
                            rs1 = rstatement.executeQuery("Select * from devicerecord where devicehash='"+deviceHash+"';");
                            rs1.next();
                            String cp_status = rs1.getString("positive_status");
                            if(cp_status.equals("true")){
                                 positive_contact = rs1.getString("deviceHash");
                                 pcontact_date = contact_date;
                                 pctest_date =  rs1.getString("ptest_date");
                                 ftcdate = daysAddition(pctest_date,14);
                                 ptcdate = daysAddition(pctest_date,-14);

                                if(WithinPositveRange(pcontact_date,ptcdate,ftcdate)){
                                    usernotified = "true";
                                    rstatement.executeUpdate("Update devicerecord SET usernotified='true' where devicehash='"+sourceHash+"';");
                                    rstatement.executeUpdate("Update devicerecord SET pcontact_date='"+contact_date+"' where devicehash='"+sourceHash+"';");
                                    comecontact = true;
                                }else{

                                    // GOTTA WORK HERE
                                }

                            }
                        }
                    }

                }
            NodeList testlist = document.getElementsByTagName("testHash");
            String testhash="";
            boolean singulartest=false;
            Calendar calendar = Calendar.getInstance();
            String exception_date = "";
            ArrayList<String> thashlist = new ArrayList<>();
            if(!document.getElementsByTagName("testHash").item(0).getTextContent().isEmpty()){


                for(int i = 0 ; i<testlist.getLength(); i++){
                    testhash = document.getElementsByTagName("testHash").item(i).getTextContent();
                    //    System.out.println(testhash);
                    resultSet = statement.executeQuery("Select TestHash,TestDevice, testDate, TestResult from TestRecord where TestHash ='"+testhash+"';");
                    if(resultSet.isBeforeFirst()) {
                        resultSet.next();
                        String deviceassigned = resultSet.getString("TestDevice");
                        String govtresult = resultSet.getString("TestResult");
                        if(govtresult.equals("true") && deviceassigned==null)
                            rstatement.executeUpdate("Update TestRecord SET TestDevice='"+sourceHash+"' where TestHash='"+testhash+"';");
                        thashlist.add(resultSet.getString("TestHash"));
                    }
                }

                System.out.println(thashlist);

                if(thashlist.size()==1){
                    resultSet = statement.executeQuery("Select TestHash,testDate, TestResult from TestRecord where TestHash ='"+testhash+"';");
                    resultSet.next();
                    ptest_date = resultSet.getString("testDate");
                    positive_status = "true";
                    statement.executeUpdate("Update devicerecord SET positive_status='"+positive_status+"' where devicehash='"+sourceHash+"';");
                    statement.executeUpdate("Update devicerecord SET ptest_date='"+ptest_date+"' where devicehash='"+sourceHash+"';");
                    rstatement.executeUpdate("Update devicerecord SET usernotified='false' where devicehash='"+sourceHash+"';");

                }else if(thashlist.size()>1){
                    Map<Integer, String> testdateregister = new HashMap<>();
                    ArrayList<Integer> keytracker = new ArrayList<>();
                    ArrayList<String> ExceptionDate = new ArrayList<>();

                    for(int i =0 ; i<thashlist.size();i++){
                        testhash = thashlist.get(0);
                        resultSet = statement.executeQuery("Select TestHash,testDate, TestResult from TestRecord where TestHash ='"+testhash+"';");
                        resultSet.next();
                        String govtresult = resultSet.getString("TestResult");
                        if(govtresult.equals("true")){
                            System.out.println("TEST HAS BEEN VALIDATED");
                            String date = resultSet.getString("testDate");
                            testdateregister.put(i,date);
                            keytracker.add(i);
                        }
                    }
                    String smalldate = testdateregister.get(0);
                    Date firstdate = new Date();
                    Date date1 = sdf.parse(smalldate);
                    for(int i =1 ; i<testdateregister.size();i++){
                        int key = keytracker.get(i);
                        Date date2 = sdf.parse(testdateregister.get(key));
                        if(date2.after(date1)){
                            firstdate = date1;
                            calendar.setTime(date1);
                            calendar.add(Calendar.DAY_OF_MONTH,14);
                            String futuredate = sdf.format(calendar.getTime());
                            Date negativedate = sdf.parse(futuredate);
                            String entry = sdf.format(negativedate);

                            if(negativedate.before(date2)){
                                ExceptionDate.add(entry);
                            }

                        }else{
                            firstdate = date2;
                            calendar.setTime(firstdate);
                            calendar.add(Calendar.DAY_OF_MONTH,14);
                            String futuredate = sdf.format(calendar.getTime());
                            Date negativedate = sdf.parse(futuredate);
                            String entry = sdf.format(negativedate);

                            if(negativedate.before(date1)){
                                ExceptionDate.add(entry);
                            }
                        }
                    }
                    Date final_date = sdf.parse("01-01-2021");
                    if(ExceptionDate.size()!=0){
                        for(int i =0 ; i<ExceptionDate.size();i++){
                            Date current_date = sdf.parse(ExceptionDate.get(i));

                            if(current_date.after(final_date)){
                                final_date = current_date;
                            }
                        }
                        exception_date = sdf.format(final_date);
                        pcontact_date = "null";
                        positive_contact = "SELF";
                        ptest_date = sdf.format(firstdate);
                        positive_status="true";
                        statement.executeUpdate("Update devicerecord SET positive_status='"+positive_status+"' where devicehash='"+sourceHash+"';");
                        statement.executeUpdate("Update devicerecord SET ptest_date='"+ptest_date+"' where devicehash='"+sourceHash+"';");
                        rstatement.executeUpdate("Update devicerecord SET usernotified='false' where devicehash='"+sourceHash+"';");

                    }else{
                        pcontact_date = "null";
                        positive_contact = "SELF";
                        ptest_date = sdf.format(firstdate);
                        positive_status="true";
                        statement.executeUpdate("Update devicerecord SET positive_status='"+positive_status+"' where devicehash='"+sourceHash+"';");
                        statement.executeUpdate("Update devicerecord SET ptest_date='"+ptest_date+"' where devicehash='"+sourceHash+"';");
                        rstatement.executeUpdate("Update devicerecord SET usernotified='false' where devicehash='"+sourceHash+"';");

                    }


                }
            }else{
                notest = true;
            }

        }

        if(comecontact) {
            return true;
        }else{
            return  false;
        }
    }



    public int findGatherings(int date, int minSize, int minTime, float density) throws SQLException {
        if(statement==null){
            ConnectionEshtablisher("");
        }
        int gatherings = 0;

        String finaldate = null;
        try {
            finaldate = daysAddition("01-01-2021",date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        resultSet= statement.executeQuery("Select source_device, contact_device, contact_date, contact_duration from contact_tracker where contact_date='"+finaldate+"'");
        Set<String> Set1 = new LinkedHashSet<>();
        Set<Set<String>> S = new LinkedHashSet<>();
        LinkedHashMap<String,Set<String>> contacts = new LinkedHashMap<>();
        LinkedHashMap<String,String> co_duration = new LinkedHashMap<>();
        while (resultSet.next()){
            String key = resultSet.getString("source_device");
            String value = resultSet.getString("contact_device");
            String combinedkey = key+value;
            String dur = resultSet.getString("contact_duration");
            co_duration.put(combinedkey,dur);

            if(contacts.containsKey(key)){
                Set<String> incoming = contacts.get(key);
                Set<String> outgoing = new LinkedHashSet<>(incoming);
                outgoing.add(value);
                contacts.put(key, outgoing);
            }else if(!contacts.containsKey(key)){
                Set<String> outgoing = new LinkedHashSet<>();
                outgoing.add(value);
                contacts.put(key,outgoing);

            }
        }
        List<String> sourcecontacts = new ArrayList(contacts.keySet());
        Set<Set<String>> testS = new LinkedHashSet<>();
        System.out.println(contacts);
        int count = 1;
        int iz=0;
        for(int i = 0 ; i<sourcecontacts.size() ; i++){
            String key = sourcecontacts.get(i);
            for(int j =count; j<sourcecontacts.size();j++){
                String nextkey = sourcecontacts.get(j);
             //   System.out.println("SET BEFORE RETAINING");
             //   System.out.println("SET A - "+SetA);
                Set<String> SetA = new LinkedHashSet<>(contacts.get(key));
                Set<String> SetB = new LinkedHashSet<>(contacts.get(nextkey));
                if(SetA.contains(nextkey) && SetB.contains(key)){
                    SetA.add(key);
                    SetB.add(nextkey);
                }
            //    System.out.println("SET B - "+ SetB);
                SetA.retainAll(SetB);

            //    System.out.println("AFTER RETAINTION");
              //  System.out.println("SET-A +"+SetA);
                S.add(SetA);
            }
            count++;
        }
        System.out.println(S.size());
        System.out.println(S);
        for (Set Sb : S){
            int indcount = 0;
            ArrayList<String> tempset = new ArrayList<>(Sb);

            int m = 0;
            int lcount = 1;
            if(Sb.size()>=minSize){
                for (int i =0; i<tempset.size();i++){
                    String first = tempset.get(i);
                    if(tempset.size()>1){
                        for(int j = lcount; j<tempset.size(); j++){
                            String second = tempset.get(j);
                            String combinedkey = first+second;
                            if(co_duration.containsKey(combinedkey)) {
                                String icduration = co_duration.get(combinedkey);
                                int cduration = Integer.parseInt(icduration);
                                if (cduration >= minTime) {
                                    indcount++;
                                }
                            }
                        }
                    }

                }
                int noofindividuals = tempset.size();
                m = noofindividuals * (noofindividuals-1)/2;
                System.out.println("THE INDCOUNT IS HERE => "+indcount);
                System.out.println("THE M IS HERE =>"+ m);
                float ratio=0;
                if(m>0) {
                    ratio = indcount / m;
                    System.out.println("RATIO IS HERE => "+ ratio);
                }
                if(ratio>=density){
                    gatherings++;

                }

            }


        }


        System.out.println(co_duration.size());
        return gatherings;
    }

    private boolean WithinPositveRange(String cdate, String ptdate, String ftdate) throws ParseException {


        Date contactdate = sdf.parse(cdate);
        Date ptest= sdf.parse(ptdate);
        Date ftest = sdf.parse(ftdate);
        return !(contactdate.before(ptest) || contactdate.after(ftest));
    }

    private String daysAddition(String date, int days) throws ParseException {

        Date formatteddate = sdf.parse(date);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(formatteddate);
        calendar.add(Calendar.DAY_OF_MONTH,days);
        String futuredate = sdf.format(calendar.getTime());
        return futuredate;

    }


    private boolean ConnectionEshtablisher(String initiator){

        Connection connection = null; // Initializing connection with null

        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // Setting up the jdbc driver to connect
        } catch (Exception e) {
            System.out.println("Error Connecting to jdbc"); // If there is any exception, we display this message
        }
        try {
            connection = DriverManager.getConnection("jdbc:mysql://db.cs.dal.ca:3306/harjot", "harjot", "B00872298"); // Setting up the connection with the server url and the user and password
            statement = connection.createStatement(); // The statement is then created on the basis of the connection
            rstatement = connection.createStatement();
            resultSet = statement.executeQuery("select * from devicerecord where devicehash ='"+initiator+"';");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }


        return true;
    }

    public void recordTestResult(String testHash, int date, boolean result) throws SQLException {
        if(testHash.isEmpty() || testHash==null || date < 1 ){
            System.out.println("Invalid Test Hash or Date");
            return;
        }
        if(statement==null){
            ConnectionEshtablisher("");
        }

        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(sdf.parse("01-01-2021"));
        }catch (ParseException pe){
            System.out.println(pe);
        }

        calendar.add(Calendar.DAY_OF_MONTH,date);
        String dateofcontact = sdf.format(calendar.getTime());

        statement.executeUpdate("Insert into TestRecord (TestHash, testDate, TestResult) Values('" + testHash + "','"+ dateofcontact + "','"+result+"')");
    }




}
