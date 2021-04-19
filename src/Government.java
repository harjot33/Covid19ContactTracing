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
                      //  String[] tdatelist = incomingpcdate.split(","); // Now we create an array of strings with "," as the delimiter from the contact_list taken from the database
                            ftcdate = daysAddition(pctest_date, 14); // We check if the date of contact is within the 14 days
                            ptcdate = daysAddition(pctest_date, -14); // We check if the date of contact is within the 14 days

                            if (WithinPositveRange(pcontact_date, ptcdate, ftcdate)) { // If contact date indeed is +-14 days since the test date
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
                //SQL Query for inserting information into the devicerecord table.
                for(int i =0 ; i<contactlist.size();i++){
                    statement.executeUpdate("INSERT INTO contact_tracker (source_device, contact_device, contact_date,contact_duration)"+ "Values('" + sourceHash + "','" + contactlist.get(i) + "','"+dateList.get(i)+"','"+durationList.get(i)+"')");
                //SQL Query for inserting information into the contact_tracker table.
                }

            }else if(notest && !comecontact){ // If there are no tests and the phone has not come in contact with anyone
                ptest_date = "null"; // Setting the p_test as null
                positive_status = "false";  // positive status is false
                positive_contact = "null"; // no positive contacts
                pcontact_date = "null"; // no positive test dates as there are no tests
                usernotified = "false"; // The usernotified is set to be false
                String contactstring = String.join(",", contactlist);
                statement.executeUpdate("INSERT INTO devicerecord (devicehash, contact_list, last_contact,positive_contact, pcontact_date, positive_status, ptest_date,usernotified)" +"Values('" + sourceHash + "','" + contactstring + "','"+last_contact+"','"+positive_contact+"','"+pcontact_date+"','"+positive_status+"','"+ptest_date+"','"+usernotified+"')");
                // This query is used to insert information into the devicerecord table
                for(int i =0 ; i<contactlist.size();i++){
                    statement.executeUpdate("INSERT INTO contact_tracker (source_device, contact_device, contact_date,contact_duration)"+ "Values('" + sourceHash + "','" + contactlist.get(i) + "','"+dateList.get(i)+"','"+durationList.get(i)+"')");
                    // This contact_tracker table has had some data inserted into the by the above statement
                }

            }else if(!comecontact){ // If there is simply no contact.
                String contactstring = String.join(",", contactlist);
                usernotified = "false"; // User notified as "false".
                statement.executeUpdate("INSERT INTO devicerecord (devicehash, contact_list, last_contact,positive_contact, pcontact_date, positive_status, ptest_date,usernotified)" + "Values('" + sourceHash + "','" + contactstring + "','" + last_contact + "','" + positive_contact + "','" + pcontact_date + "','" + positive_status + "','" + ptest_date+"','"+usernotified + "')");
                // This query is used to insert information into the devicerecord table
                for(int i =0 ; i<contactlist.size();i++){
                    statement.executeUpdate("INSERT INTO contact_tracker (source_device, contact_device, contact_date,contact_duration)"+ "Values('" + sourceHash + "','" + contactlist.get(i) + "','"+dateList.get(i)+"','"+durationList.get(i)+"')");
                    // This contact_tracker table has had some data inserted into the by the above statement

                }

            }else if(comecontact){ // If the device has come in contact with someone who has tested positive for the covid.
                String contactstring = String.join(",", contactlist);
                usernotified = "true";
                rstatement.executeUpdate("Update devicerecord SET usernotified='true' where devicehash='"+sourceHash+"';");
                //Updating the usernotified as true.
                statement.executeUpdate("INSERT INTO devicerecord (devicehash, contact_list, last_contact,positive_contact, pcontact_date, positive_status, ptest_date, usernotified)" +"Values('" + sourceHash + "','" + contactstring + "','"+last_contact+"','"+positive_contact+"','"+pcontact_date+"','"+positive_status+"','"+ptest_date+"','"+usernotified+"')");
                //This query is used to insert information into the devicerecord table
                for(int i =0 ; i<contactlist.size();i++){
                    statement.executeUpdate("INSERT INTO contact_tracker (source_device, contact_device, contact_date,contact_duration)"+ "Values('" + sourceHash + "','" + contactlist.get(i) + "','"+dateList.get(i)+"','"+durationList.get(i)+"')");
                    // This contact_tracker table has had some data inserted into the by the above statement
                }


            }


        }else { // If there are entries already in the source hash device.
            boolean notif = false; // notif is set to be false
            NodeList ok = document.getElementsByTagName("device"); // Getting the nodelist ok as the elements of the "device" are taken
            ArrayList<String> contactlist = new ArrayList<>(); // contactlist is intialized
            ArrayList<String> datelist = new ArrayList<>(); // datelist is initialized
            ArrayList<String> durationlist = new ArrayList<>();// durationlisti s intialized
            // Below Strings are used in the following code to give information about the table and what its columns are.
            String duplicateverification;
            String lastcontactDB;
            String lastcontactLO;
            String usernotified;
            String positive_status = "";
            String positive_contact ;
            String pcontact_date;
            String pctest_date="";
            String ftcdate ;
            String ptcdate;
            String ptest_date="";
            resultSet.next();
            for(int i =0 ; i < ok.getLength(); i++){ // In this iterative statement, we loop over the previously created Document elements as we add to the contact, date and duration arraylists.
                String deviceHash  =document.getElementsByTagName("deviceHash").item(i).getTextContent();
                contactlist.add(deviceHash);
                String date = document.getElementsByTagName("date").item(i).getTextContent();
                datelist.add(date);
                String duration = document.getElementsByTagName("duration").item(i).getTextContent();
                durationlist.add(duration);
            }

            duplicateverification = resultSet.getString("contact_list"); // We firstly get the contact_list from the  database
            lastcontactDB = resultSet.getString("last_contact"); // Then we get the last contact from the database
            positive_status = resultSet.getString("positive_status");// It is followed by the positive status of the source device.
            lastcontactLO = contactlist.get(contactlist.size()-1); // We further retrieve the lastcontact that is there on the local device.
            String[] devices = duplicateverification.split(","); // Now we create an array of strings with "," as the delimiter from the contact_list taken from the database
            List<String> devicelist = new ArrayList<String>(Arrays.asList(devices)); // Further convert the array into a list
            usernotified = resultSet.getString("usernotified"); // Retreieve the usernotified status from the database.
            String newclist = duplicateverification; // Taking the contact list retrieved from the database into a string
            String lastcontact="";// initializing lastcontact as empty string


            if(devices.length == contactlist.size() && lastcontactDB.equals(lastcontactLO)){ // This is done to ensure that same contacts are not added again and again, hence causing duplicacy, these two checks enforce that only new contacts are added.
                if(usernotified.equals("true")){ // If the usernotified equals true, we set the notif boolean as true
                    notif = true;
                }
                System.out.println("No new contacts."); // Print No new contacts message.
            }else { // If there are new contacts, the following code sequence is executed.
                int beg = contactlist.indexOf(lastcontactDB); // The lastcontact of the local data is retrieved
                beg++; // Increment by one to look at the index of the new contact
                for (int i = beg; i < contactlist.size(); i++) { // Then we loop from that point to the end of the list
                    String incominghash = contactlist.get(i); // Retrieving the new contacts from the list one by one as the loop iterates.

                    if(devicelist.contains(incominghash)){ // This statement is for those conditions when a previously added contact has been added again.

                        resultSet  = statement.executeQuery("Select source_device,contact_date,contact_duration from contact_tracker where contact_device='"+incominghash+"'");
                        resultSet.next(); // Shifting to the next row which actually points to the data.
                        String cdate = resultSet.getString("contact_date");// Getting the contact_date
                        if(cdate.equals(datelist.get(i))){ // If the contact_date equals to a date in the datelist - there is already a contact with the same device on the given date.
                            // We add the duration of both the contacts as they happen on the same date.
                            String duration = resultSet.getString("contact_duration"); // We retrieve the contact duration from the database
                            int dur = Integer.parseInt(duration); // Parse it into an integer
                            String ldur = durationlist.get(i);// We retrieve the local duration
                            int dur1 = Integer.parseInt(ldur);// Parse it into an integer
                            dur = dur+dur1; // add the duration
                            String finaldur = dur+"";
                            rstatement.executeUpdate("Update contact_tracker SET contact_duration='"+finaldur+"' where contact_duration='"+duration+"' AND source_device='"+sourceHash+"';");
                            // Here we have updated the existing contact on the database and added the duration as the contacts were happening on the same day.
                        }else{ // If the same contact happened on a different day.
                            if(i==contactlist.size()-1){
                                lastcontact = incominghash; // Getting the last contact
                            }
                            newclist=newclist+","+incominghash;
                            rstatement.executeUpdate("INSERT INTO contact_tracker (source_device, contact_device, contact_date,contact_duration)"+ "Values('" + sourceHash + "','" + contactlist.get(i) + "','"+datelist.get(i)+"','"+durationlist.get(i)+"')");
                            // Inserting the new contact into the database.

                        }

                    }else{ // If the contact happened on a different day, new contacts happening on a different day.
                        if(i==contactlist.size()-1){
                             lastcontact = incominghash; // Getting the last contact
                        }
                        newclist=newclist+","+incominghash;
                        rstatement.executeUpdate("INSERT INTO contact_tracker (source_device, contact_device, contact_date,contact_duration)"+ "Values('" + sourceHash + "','" + contactlist.get(i) + "','"+datelist.get(i)+"','"+durationlist.get(i)+"')");
                        // Inserting the new contact into the database.
                    }
                    String contact_date = ""; // Initializing contact_date with empty string.
                    rs1 = rstatement.executeQuery("Select * from devicerecord where deviceHash='"+incominghash+"';"); // Here we get the device details of the device in the contact list.
                    resultSet = statement.executeQuery("Select * from contact_tracker where source_device='"+sourceHash+"' AND contact_device='"+incominghash+"';"); // We further get the contacts between the source device and contact device
                    if(resultSet.isBeforeFirst()){
                        resultSet.next();
                        contact_date = resultSet.getString("contact_date");
                    } // Getting the date of contact.
                    String cp_status = "";
                    if(rs1.isBeforeFirst()){
                        rs1.next();
                        cp_status = rs1.getString("positive_status"); // Getting the positive status of the contact
                    }
                    if(cp_status.equals("true")){ // If its true we then check the date of the contact and compare it with the test date of the device which has test positive, using the absolute value.
                        positive_contact = rs1.getString("deviceHash");
                        pcontact_date = contact_date;
                         pctest_date =  rs1.getString("ptest_date");
                         // Now we create an array of strings with "," as the delimiter from the contact_list taken from the database
                            ftcdate = daysAddition(pctest_date, 14); // We check if the date of contact is within the 14 days
                            ptcdate = daysAddition(pctest_date, -14); // We check if the date of contact is within the 14 days

                            if (WithinPositveRange(pcontact_date, ptcdate, ftcdate)) { // If it is found that it is within the absolute range +-14 Days, then we say that the device has come in contact with a positive person.
                                positive_status = "false";
                                ptest_date = "null";
                                comecontact = true; // We set the comecontact boolean to be true to signal that it has been in contact with a positive person
                                usernotified = "true"; // User has been notified.
                                rstatement.executeUpdate("Update devicerecord SET usernotified='true' where devicehash='" + sourceHash + "';"); // Update the same in the database
                            }


                    }

                }
                rstatement.executeUpdate("Update devicerecord SET contact_list='"+newclist+"' where devicehash='"+sourceHash+"';"); // We update the contactlist with the new contacts.
                rstatement.executeUpdate("Update devicerecord SET last_contact='"+lastcontact+"' where devicehash='"+sourceHash+"';"); // We add the last contact according to the newly added contacts.


            }
            for(int i = 0 ; i<contactlist.size(); i++){
                String incominghash = contactlist.get(i);
                rs1 = statement.executeQuery("Select * from devicerecord where devicehash='"+incominghash+"'");
                rs1.next();
                positive_status = rs1.getString("positive_status");
                if(positive_status == "true"){
                    comecontact = true;
                    rstatement.executeUpdate("Update devicerecord SET usernotified='true' where devicehash='" + sourceHash + "';"); // Update the same in the database
                }

            }

            if(usernotified.equals("false") && positive_status.equals("false")){ // If the user has not been notified and they have not tested positive
                resultSet.close();// We firstly close the resultset
                resultSet = statement.executeQuery("Select source_device,contact_date,contact_duration from contact_tracker where contact_device='"+sourceHash+"'");
                // Then we get the device contact record from the database.
                if(resultSet.isBeforeFirst()){ // if the resulset is not empty then
                        while (resultSet.next()){ // perform the follow operations while the resultset has a next row.
                            String deviceHash = resultSet.getString("source_device"); // we get the devicehash string.
                            String contact_date = resultSet.getString("contact_date"); // we then get the contact_date of the contact
                            String contact_duration = resultSet.getString("contact_duration"); // get the duration of the contact
                            rs1 = rstatement.executeQuery("Select * from devicerecord where devicehash='"+deviceHash+"';");
                            // Use the database to get the details of the contact device
                            rs1.next();
                            String cp_status = rs1.getString("positive_status");
                            if(cp_status.equals("true")){ // If any of the device has tested positive, then we use the absolute value of its test date to determine whether the host device has come in contact with it or not
                                 positive_contact = rs1.getString("deviceHash");
                                 pcontact_date = contact_date;
                                String incomingpcdate =  rs1.getString("ptest_date");
                                String[] tdatelist = incomingpcdate.split(","); // Now we create an array of strings with "," as the delimiter from the contact_list taken from the database
                                for(int i =0 ; i <tdatelist.length; i++) {
                                    pctest_date = tdatelist[i];
                                    ftcdate = daysAddition(pctest_date, 14); // We check if the date of contact is within the 14 days
                                    ptcdate = daysAddition(pctest_date, -14); // We check if the date of contact is within the 14 days

                                    if (WithinPositveRange(pcontact_date, ptcdate, ftcdate)) {// If it is found that it is within the absolute range +-14 Days, then we say that the device has come in contact with a positive person.
                                        usernotified = "true";
                                        rstatement.executeUpdate("Update devicerecord SET usernotified='true' where devicehash='" + sourceHash + "';"); // User notified updated
                                        rstatement.executeUpdate("Update devicerecord SET pcontact_date='" + contact_date + "' where devicehash='" + sourceHash + "';");// Positive Contact Date has been updated.
                                        comecontact = true;
                                        break;
                                    }
                                }

                            }
                        }
                    }

                }
            NodeList testlist = document.getElementsByTagName("testHash"); // Getting the testlist from the element "testhash" of the document
            String testhash="";
            boolean singulartest=false; // Set the singulartest boolean as false
            Calendar calendar = Calendar.getInstance(); // Getting the calendar instance
            String exception_date = ""; // This string stores the exception date of the test.
            ArrayList<String> thashlist = new ArrayList<>(); // Initializing the thashlist arraylist
            if(!document.getElementsByTagName("testHash").item(0).getTextContent().isEmpty()){ // If there are elements in the document with the tagname testhash.


                for(int i = 0 ; i<testlist.getLength(); i++){ // Loop through the testlist.
                    testhash = document.getElementsByTagName("testHash").item(i).getTextContent(); // Get the tests one by one from the elements.
                    resultSet = statement.executeQuery("Select TestHash,TestDevice, testDate, TestResult from TestRecord where TestHash ='"+testhash+"';"); // Retrieving the test date from the database.
                    if(resultSet.isBeforeFirst()) {
                        resultSet.next();
                        String deviceassigned = resultSet.getString("TestDevice");
                        String govtresult = resultSet.getString("TestResult");
                        if(govtresult.equals("true") && deviceassigned==null) // If it is found that a positive result is there but no device has been assigned, assign the device to a positive test.
                            rstatement.executeUpdate("Update TestRecord SET TestDevice='"+sourceHash+"' where TestHash='"+testhash+"';");
                        thashlist.add(resultSet.getString("TestHash"));
                    }
                }


                if(thashlist.size()==1){ // If there is only 1 test done by the device.
                    // Retrieve the data from the testrecord table of the database.
                    // Then we set the positive status of the device as true
                    // Followed by setting the positive test date.
                    // Then we set the usernotified to be false.
                    resultSet = statement.executeQuery("Select TestHash,testDate, TestResult from TestRecord where TestHash ='"+testhash+"';");
                    resultSet.next();
                    ptest_date = resultSet.getString("testDate");
                    positive_status = "true";
                    statement.executeUpdate("Update devicerecord SET positive_status='"+positive_status+"' where devicehash='"+sourceHash+"';");
                    statement.executeUpdate("Update devicerecord SET ptest_date='"+ptest_date+"' where devicehash='"+sourceHash+"';");
                    rstatement.executeUpdate("Update devicerecord SET usernotified='false' where devicehash='"+sourceHash+"';");

                }else if(thashlist.size()>1){

                    // If there are more than 1 test done by the device.
                    // Retrieve the data from the testrecord table of the database.
                    // Then we set the positive status of the device as true
                    // Followed by setting the positive test date.
                    // Then we set the usernotified to be false.
                    Map<Integer, String> testdateregister = new HashMap<>();
                    ArrayList<Integer> keytracker = new ArrayList<>();
                    ArrayList<String> ExceptionDate = new ArrayList<>();
                    ArrayList<String> testDateAddition = new ArrayList<>();

                    for(int i =0 ; i<thashlist.size();i++){
                        testhash = thashlist.get(0);
                        resultSet = statement.executeQuery("Select TestHash,testDate, TestResult from TestRecord where TestHash ='"+testhash+"';");
                        //Retriev the test record of the device
                        resultSet.next();
                        String govtresult = resultSet.getString("TestResult");
                        if(govtresult.equals("true")){ // We add the positive test hashes of the device in the following code sequence
                            String date = resultSet.getString("testDate");
                            testDateAddition.add(date);
                            testdateregister.put(i,date);
                            keytracker.add(i);
                        }
                    }

                    ptest_date = String.join(",", testDateAddition);
                    positive_status="true";
                    // Updating the database with the positive status, ptest_date and the usernotified strings.
                    statement.executeUpdate("Update devicerecord SET positive_status='"+positive_status+"' where devicehash='"+sourceHash+"';");
                    statement.executeUpdate("Update devicerecord SET ptest_date='"+ptest_date+"' where devicehash='"+sourceHash+"';");
                    rstatement.executeUpdate("Update devicerecord SET usernotified='false' where devicehash='"+sourceHash+"';");
                }
            }

        }


        if(comecontact) { // If the user has come in contact with a positive person, we will return true.
            return true;
        }else{
            return  false; // Return false if there has been no contact with a positive person.
        }
    }



    public int findGatherings(int date, int minSize, int minTime, float density) throws SQLException { // This method is used to find the number of gatherings at a particular day when the individuals are equal
        // or above to minsize individuals and they meet for a required mintime. It also depends on the density which is the ratio of c/m - where c is the individual pairs who met for mintime and m is the maximum number of intersectional pairs.
        if(date<1 || minSize <0 || minTime<1 || density < 0){ // Input Validation - This is used to check
            return 0;
        }

        if(statement==null){ // If the statement is null
            ConnectionEshtablisher(""); // We call the ConnectionEshtablisher method.
        }
        int gatherings = 0; // Initialize gatherings with 0.

        String finaldate = null; // The finaldate is initialize with 0
        try {
            finaldate = daysAddition("01-01-2021",date); // We add the date to the finaldate to get the proper date of the gathering.
        } catch (ParseException e) {
            e.printStackTrace();
        }
        resultSet= statement.executeQuery("Select source_device, contact_device, contact_date, contact_duration from contact_tracker where contact_date='"+finaldate+"'");
        // We use the above query to get the contact details of the devices who came in contact on the given date.
        Set<Set<String>> S = new LinkedHashSet<>(); // The global Set S which contains the pairs of the intersected individuals
        LinkedHashMap<String,Set<String>> contacts = new LinkedHashMap<>(); // HashMap contacts is used to store the devices as keys and their contacts as the keys.
        LinkedHashMap<String,String> co_duration = new LinkedHashMap<>(); // This HashMap is used to store the duration of contact of the devices.
        while (resultSet.next()){ // This is used to iterate to the next row of the resultSet
            String key = resultSet.getString("source_device"); // This stores the source device as key
            String value = resultSet.getString("contact_device"); // This stores the contact device as value
            String combinedkey = key+value; // The combined key+value pair is stored as a string
            String dur = resultSet.getString("contact_duration"); // This stores the duration.
            co_duration.put(combinedkey,dur); // We put the combined key value pair and their duration into this hashmap

            if(contacts.containsKey(key)){ // Storing the Device as key and the contacts as its value
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
        List<String> sourcecontacts = new ArrayList(contacts.keySet()); // We take the keyset of contacts and then store it into a list
        int count = 1; // Count = 1 - Initialized
        for(int i = 0 ; i<sourcecontacts.size() ; i++){ // We iterate through the devices, 1 by 1, making sure no duplicate pairs of the devices are called.
            String key = sourcecontacts.get(i); // We follow an iterative approach to such that first device is the first key
            for(int j =count; j<sourcecontacts.size();j++){
                String nextkey = sourcecontacts.get(j); // The next key is the device which is on the next index.

                Set<String> SetA = new LinkedHashSet<>(contacts.get(key)); // We get the contacts present in the first key as Set A
                Set<String> SetB = new LinkedHashSet<>(contacts.get(nextkey)); // We get the contacts present in the second key as Set B
                if(SetA.contains(nextkey) && SetB.contains(key)){ // To see if the source devices contact each other or not
                    SetA.add(key); // If they do we add them in their respective sets.
                    SetB.add(nextkey);
                }
                SetA.retainAll(SetB); // We then perform an intersection, where the common sets are then taken into Set A.
                S.add(SetA); // Add Set A to gloal Set S
            }
            count++;
        }

        for (Set Sb : S){ // Iterating for every set in the global set S
            int c = 0;
            ArrayList<String> tempset = new ArrayList<>(Sb); // ArrayList tempset contains sets of S iteratively

            int m = 0;
            int lcount = 1;
            if(Sb.size()>=minSize){ // We check if the number of individuals in the set are equal to or more than the minsize.
                for (int i =0; i<tempset.size();i++){ // Then we iterate over them
                    String first = tempset.get(i);
                    if(tempset.size()>1){ // if the tempset is greater than 1.
                        for(int j = lcount; j<tempset.size(); j++){
                            String second = tempset.get(j);
                            String combinedkey = first+second;
                            if(co_duration.containsKey(combinedkey)) { // The combined key-value pair is used to get the duration of the contact
                                String icduration = co_duration.get(combinedkey);
                                int cduration = Integer.parseInt(icduration);
                                if (cduration >= minTime) { // If the duration of the contact is more or equal to the mintime
                                    c++; // we increment the c counter.
                                }
                            }
                        }
                    }

                }
                int noofindividuals = tempset.size(); // Getting the no of individuals
                m = noofindividuals * (noofindividuals-1)/2; // Getting the max number of individual pairs.
                float ratio=0;
                if(m>0) {
                    ratio = c / m;
                    // The ratio of c/m
                }
                if(ratio>=density){ // If the ratio is greater or equal to the density
                    gatherings++; // Increment gatherings

                }

            }


        }


        return gatherings; // Return the gatherings.
    }

    private boolean WithinPositveRange(String cdate, String ptdate, String ftdate) throws ParseException { // This method is used to check if the given date is within the given range


        Date contactdate = sdf.parse(cdate);
        Date ptest= sdf.parse(ptdate);
        Date ftest = sdf.parse(ftdate);
        return !(contactdate.before(ptest) || contactdate.after(ftest));
    }

    private String daysAddition(String date, int days) throws ParseException { // This method is used to add days to a given date.

        Date formatteddate = sdf.parse(date);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(formatteddate);
        calendar.add(Calendar.DAY_OF_MONTH,days);
        String futuredate = sdf.format(calendar.getTime());
        return futuredate;

    }


    private boolean ConnectionEshtablisher(String initiator){ // This method is used to establish connection to the database.

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

    public void recordTestResult(String testHash, int date, boolean result) throws SQLException { // This method is used to record the tests onto the government database server.
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
