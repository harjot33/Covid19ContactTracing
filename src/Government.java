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

public class Government {
    Properties configFile;
    ResultSet resultSet = null; // Initializing resultSet with null
    Statement statement = null; // Initializing statement with null
    Statement rstatement = null;
    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

    Government(Properties configFile){
            this.configFile = configFile;
    }

    boolean mobileContact(String initiator, String contactInfo) throws SQLException, ParserConfigurationException, IOException, SAXException, ParseException {
        ConnectionEshtablisher(initiator);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse( new InputSource( new StringReader(contactInfo)));
        boolean notest = false;
        String sourceHash = document.getElementsByTagName("sourceHash").item(0).getTextContent();
        boolean comecontact = false;
        ResultSet rs1 = null;



        if (!resultSet.isBeforeFirst() ) {
            NodeList testlist = document.getElementsByTagName("testHash");
            String testhash="";
            ArrayList<String> contactlist = new ArrayList<>();
            ArrayList<String> dateList = new ArrayList<>();
            ArrayList<String> durationList = new ArrayList<>();
            boolean singulartest = false;
            String positive_contact ="";
            String pcontact_date = "";
            String positive_status = "";
            String ptest_date = "";
            String exception_date = "";
            String pctest_date="";
            String ftcdate="";
            String ptcdate="";
            String last_contact = "";
            String usernotified="";

            Calendar calendar = Calendar.getInstance();


            resultSet = statement.executeQuery("Select source_device,contact_date,contact_duration from contact_tracker where contact_device='"+sourceHash+"'");
            if(resultSet.isBeforeFirst()){
                while (resultSet.next()){

                    String deviceHash = resultSet.getString("source_device");
                    String contact_date = resultSet.getString("contact_date");
                    String contact_duration = resultSet.getString("contact_duration");
                    rs1 = rstatement.executeQuery("Select * from devicerecord where deviceHash='"+deviceHash+"';");
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
                        }else{

                            // GOTTA WORK HERE
                        }

                    }
                }
            }


            if(!document.getElementsByTagName("testHash").item(0).getTextContent().isEmpty()){
                ArrayList<String> thashlist = new ArrayList<>();

                for(int i = 0 ; i<testlist.getLength(); i++){
                    testhash = document.getElementsByTagName("testHash").item(i).getTextContent();
                    //    System.out.println(testhash);
                    resultSet = statement.executeQuery("Select TestHash,testDate, TestResult from TestRecord where TestHash ='"+testhash+"';");

                    if(resultSet.isBeforeFirst()) {
                        resultSet.next();
                        String govtresult = resultSet.getString("TestResult");
                        if(govtresult.equals("true"))
                            statement.executeUpdate("Update TestResult SET TestDevice='"+sourceHash+"' where TestHash='"+testhash+"';");
                        thashlist.add(resultSet.getString("TestHash"));
                    }
                }
                System.out.println(thashlist);

                if(thashlist.size()==1 && thashlist.get(0).equals(document.getElementsByTagName("testHash").item(0).getTextContent())){
                    String govtresult = resultSet.getString("TestResult");
                    if(govtresult.equals("true")){
                        System.out.println("TEST HAS BEEN VALIDATED");
                        singulartest  = true;
                    }

                }else if(thashlist.size()>1){
                    Map<Integer, String> testdateregister = new HashMap<>();
                    ArrayList<Integer> keytracker = new ArrayList<>();
                    ArrayList<String> ExceptionDate = new ArrayList<>();

                    for(int i =0 ; i<thashlist.size();i++){
                        testhash = thashlist.get(0);
                        resultSet = statement.executeQuery("Select TestHash,testDate, TestResult from TestRecord where TestHash ='"+testhash+"';");
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
                        Date date2 = sdf.parse(testdateregister.get(i));
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
                    }else{
                        pcontact_date = "null";
                        positive_contact = "SELF";
                        ptest_date = sdf.format(firstdate);
                        positive_status="true";
                    }


                }
            }else{
                notest = true;
            }

            NodeList ok = document.getElementsByTagName("device");

         //   System.out.println(document.getElementsByTagName("testHash").item(0).getTextContent());
            for(int i =0 ; i < ok.getLength(); i++){
                        String deviceHash  =document.getElementsByTagName("deviceHash").item(i).getTextContent();
                        contactlist.add(deviceHash);
                        String date = document.getElementsByTagName("date").item(i).getTextContent();
                        dateList.add(date);
                        String duration = document.getElementsByTagName("duration").item(i).getTextContent();
                        durationList.add(duration);
                        System.out.println(deviceHash+"-"+date+"-"+duration+"-"+positive_status);
                    }



            // statement.executeUpdate("INSERT INTO devicerecord (devicehash, contact_list, last_contact)\n" +"Values('" + sourceHash + "','" + contactlistHash + "','"+result+"')");
            if(contactlist.size()>0){
                 last_contact = contactlist.get(contactlist.size()-1);

            }
            if(singulartest && !comecontact){
              //  positive_contact = resultSet.getString("")
                ptest_date = resultSet.getString("testDate");
                positive_status = resultSet.getString("TestResult");
                positive_contact = "SELF";
                pcontact_date = "null";
                usernotified = "true";
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
                usernotified = "true";
                statement.executeUpdate("INSERT INTO devicerecord (devicehash, contact_list, last_contact,positive_contact, pcontact_date, positive_status, ptest_date,usernotified)" + "Values('" + sourceHash + "','" + contactstring + "','" + last_contact + "','" + positive_contact + "','" + pcontact_date + "','" + positive_status + "','" + ptest_date+"','"+usernotified + "')");
                for(int i =0 ; i<contactlist.size();i++){
                    statement.executeUpdate("INSERT INTO contact_tracker (source_device, contact_device, contact_date,contact_duration)"+ "Values('" + sourceHash + "','" + contactlist.get(i) + "','"+dateList.get(i)+"','"+durationList.get(i)+"')");
                }

            }else if(comecontact){
                String contactstring = String.join(",", contactlist);
                usernotified = "true";
                statement.executeUpdate("INSERT INTO devicerecord (devicehash, contact_list, last_contact,positive_contact, pcontact_date, positive_status, ptest_date, usernotified)" +"Values('" + sourceHash + "','" + contactstring + "','"+last_contact+"','"+positive_contact+"','"+pcontact_date+"','"+positive_status+"','"+ptest_date+"','"+usernotified+"')");
                for(int i =0 ; i<contactlist.size();i++){
                    statement.executeUpdate("INSERT INTO contact_tracker (source_device, contact_device, contact_date,contact_duration)"+ "Values('" + sourceHash + "','" + contactlist.get(i) + "','"+dateList.get(i)+"','"+durationList.get(i)+"')");
                }


            }


        }else {
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
                    }else{
                        pcontact_date = "null";
                        positive_contact = "SELF";
                        ptest_date = sdf.format(firstdate);
                        positive_status="true";
                        statement.executeUpdate("Update devicerecord SET positive_status='"+positive_status+"' where devicehash='"+sourceHash+"';");
                        statement.executeUpdate("Update devicerecord SET ptest_date='"+ptest_date+"' where devicehash='"+sourceHash+"';");
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

        String finaldate = null;
        try {
            finaldate = daysAddition("01-01-2021",date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        resultSet= statement.executeQuery("Select source_device, contact_device, contact_date, contact_duration from contact_tracker where contact_date='"+finaldate+"'");
        Set<String> Set1 = new LinkedHashSet<>();
        LinkedHashMap<String,ArrayList<String>> contacts = new LinkedHashMap<>();
        while (resultSet.next()){
            String key = resultSet.getString("source_device");
            String value = resultSet.getString("contact_device");

            if(contacts.containsKey(key)){
                ArrayList<String> incoming = contacts.get(key);
                ArrayList<String> outgoing = new ArrayList<>(incoming);
                if(!incoming.contains(value)) {
                    outgoing.add(value);
                    contacts.put(key, outgoing);
                }
            }else if(!contacts.containsKey(key)){
                if(contacts.containsKey(value)){
                    ArrayList<String> incoming = contacts.get(value);

                    if(!incoming.contains(key)){
                        ArrayList<String> outgoing = new ArrayList<>();
                        outgoing.add(value);
                        contacts.put(key,outgoing);
                    }
                }else{
                    ArrayList<String> outgoing = new ArrayList<>();
                    outgoing.add(value);
                    contacts.put(key,outgoing);
                }
            }
        }
        System.out.println(contacts.);

        return 1;
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
