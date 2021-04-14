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

    Government(Properties configFile){
            this.configFile = configFile;
    }

    boolean mobileContact(String initiator, String contactInfo) throws SQLException, ParserConfigurationException, IOException, SAXException, ParseException {
        ConnectionEshtablisher();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse( new InputSource( new StringReader(contactInfo)));
        boolean notest = false;
        String sourceHash = document.getElementsByTagName("sourceHash").item(0).getTextContent();


        if (!resultSet.isBeforeFirst() ) {


            NodeList testlist = document.getElementsByTagName("testHash");
            String testhash="";
            ArrayList<String> contactlist = new ArrayList<>();
            boolean singulartest = false;
            String positive_contact ="";
            String pcontact_date = "";
            String positive_status = "";
            String ptest_date = "";
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
            String exception_date = "";
            Calendar calendar = Calendar.getInstance();

            if(!document.getElementsByTagName("testHash").item(0).getTextContent().isEmpty()){
                ArrayList<String> thashlist = new ArrayList<>();

                for(int i = 0 ; i<testlist.getLength(); i++){
                    testhash = document.getElementsByTagName("testHash").item(i).getTextContent();
                    //    System.out.println(testhash);
                    resultSet = statement.executeQuery("Select TestHash,testDate, TestResult from TestRecord where TestHash ='"+testhash+"';");

                    if(resultSet.isBeforeFirst())
                        resultSet.next();
                        thashlist.add(resultSet.getString("TestHash"));
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
                            date1 = date2;
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

                        pcontact_date = sdf.format(firstdate);
                        positive_contact = "SELF";
                        ptest_date = sdf.format(firstdate);
                        positive_status="true";
                    }else{
                        pcontact_date = sdf.format(firstdate);
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
                        String duration = document.getElementsByTagName("duration").item(i).getTextContent();
                        System.out.println(deviceHash+"-"+date+"-"+duration+"-"+positive_status);
                    }



            // statement.executeUpdate("INSERT INTO devicerecord (devicehash, contact_list, last_contact)\n" +"Values('" + sourceHash + "','" + contactlistHash + "','"+result+"')");
            String last_contact = contactlist.get(contactlist.size()-1);
            if(singulartest){
              //  positive_contact = resultSet.getString("")
                ptest_date = resultSet.getString("testDate");
                positive_status = resultSet.getString("TestResult");
                positive_contact = "SELF";
                pcontact_date = ptest_date;
                String contactstring = String.join(",", contactlist);
                statement.executeUpdate("INSERT INTO devicerecord (devicehash, contact_list, last_contact,positive_contact, pcontact_date, positive_status, ptest_date)" +"Values('" + sourceHash + "','" + contactstring + "','"+last_contact+"','"+positive_contact+"','"+pcontact_date+"','"+positive_status+"','"+ptest_date+"')");
            }else if(notest){
                ptest_date = "null";
                positive_status = "false";
                positive_contact = "null";
                pcontact_date = "null";
                String contactstring = String.join(",", contactlist);
                statement.executeUpdate("INSERT INTO devicerecord (devicehash, contact_list, last_contact,positive_contact, pcontact_date, positive_status, ptest_date)" +"Values('" + sourceHash + "','" + contactstring + "','"+last_contact+"','"+positive_contact+"','"+pcontact_date+"','"+positive_status+"','"+ptest_date+"')");


            }


        }else {
            resultSet = statement.executeQuery("Select * from devicerecord where devicehash='"+ sourceHash+"';");
            resultSet.next();
            System.out.println(resultSet.getString("devicehash"));




        }




        return true;
    }


    private boolean ConnectionEshtablisher(){

        Connection connection = null; // Initializing connection with null

        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // Setting up the jdbc driver to connect
        } catch (Exception e) {
            System.out.println("Error Connecting to jdbc"); // If there is any exception, we display this message
        }
        try {
            connection = DriverManager.getConnection("jdbc:mysql://db.cs.dal.ca:3306/harjot", "harjot", "B00872298"); // Setting up the connection with the server url and the user and password
            statement = connection.createStatement(); // The statement is then created on the basis of the connection
            resultSet = statement.executeQuery("select * from devicerecord;");
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

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(sdf.parse("01-01-2021"));
        }catch (ParseException pe){
            System.out.println(pe);
        }

        calendar.add(Calendar.DAY_OF_MONTH,date);
        String dateofcontact = sdf.format(calendar.getTime());

        statement.executeUpdate("Insert into TestRecord (TestHash, testDate, TestResult) Values('" + testHash + "','" + dateofcontact + "','"+result+"')");
    }


}
