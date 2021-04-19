import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Properties;

public class UnitTest {


    @Test
    @DisplayName("Input Validation Tests")
    public void inputValidationTest() throws Exception {
        Properties govtproperties = new Properties(); // We create a properties object to define the properties of the Government.
        govtproperties.setProperty("user","harjot"); // Used to store the username
        govtproperties.setProperty("password","B00872298"); // Used to store the password
        govtproperties.setProperty("url","jdbc:mysql://db.cs.dal.ca:3306/harjot"); //// Used to store the Database URL

        Government gov = new Government(govtproperties);
        Properties config = new Properties(); // We create the config of the first mobile device
        config.setProperty("address", "127.192.1.1"); // Defining the address of the mobile device
        config.setProperty("deviceName", "Oneplus 3T"); // Defining the name of the mobile device
        String unique = config.getProperty("address") + config.getProperty("deviceName");
        MobileDevice mobile = new MobileDevice(config,gov);
        mobile.recordContact("",0,0);
        mobile.positiveTest("");
        gov.recordTestResult("",0,false);
    }

    @Test
    @DisplayName("Testing Mobile Contact")
    public void mobileContact() throws Exception {
        String inputGov_local = "config/gov_local.txt";
        String inputMob = "config/mobile.txt";

        Properties govtproperties = new Properties(); // We create a properties object to define the properties of the Government.
        govtproperties.setProperty("user","harjot"); // Used to store the username
        govtproperties.setProperty("password","B00872298"); // Used to store the password
        govtproperties.setProperty("url","jdbc:mysql://db.cs.dal.ca:3306/harjot"); //// Used to store the Database URL

        Government gov = new Government(govtproperties);
        Properties config = new Properties(); // We create the config of the first mobile device
        config.setProperty("address", "127.192.1.1"); // Defining the address of the mobile device
        config.setProperty("deviceName", "Oneplus 3T"); // Defining the name of the mobile device
        String unique = config.getProperty("address") + config.getProperty("deviceName");
        MobileDevice mobile = new MobileDevice(config,gov);

        mobile.recordContact("BB",3,3);
        mobile.recordContact("BB",3,4);

        Assertions.assertFalse(mobile.synchronizeData());

    }


}