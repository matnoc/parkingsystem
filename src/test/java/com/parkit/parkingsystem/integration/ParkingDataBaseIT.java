package com.parkit.parkingsystem.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.parkit.parkingsystem.config.DataBaseConfig;
import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @Mock
    private static DataBaseConfig dataBaseConfig;

    @Mock
    private ParkingService parkingService;



    @BeforeAll
    private static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        lenient().when(inputReaderUtil.readSelection()).thenReturn(1);
        lenient().when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        
    }

    @AfterAll
    private static void tearDown(){

    }

    @Test
    public void testParkingACar(){
        parkingService.processIncomingVehicle();

        Connection con = null;
            int visitNumber = -50;
            boolean exist = false;
        try {
            con = dataBaseTestConfig.getConnection();
            PreparedStatement ps = con.prepareStatement("select p.AVAILABLE from ticket t, parking p where t.VEHICLE_REG_NUMBER ='ABCDEF' and t.PARKING_NUMBER=p.PARKING_NUMBER and p.Type ='CAR'");
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                exist = rs.getBoolean(1);
            }
            con = dataBaseTestConfig.getConnection();
            ps = con.prepareStatement("select count(*) from ticket t, parking p where t.VEHICLE_REG_NUMBER ='ABCDEF' and t.PARKING_NUMBER=p.PARKING_NUMBER and p.Type ='CAR'");
            rs = ps.executeQuery();
            if(rs.next()){
                visitNumber = rs.getInt(1);
            }
        }catch (Exception ex){
        }
        assertEquals( 1 ,  visitNumber);
        assertEquals( exist , false );
    }

     @Test
    public void testParkingLotExit(){

        Ticket ticket = new Ticket();
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - (  24 * 60 * 60 * 1000) );
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);
        ticket.setId(1);
        ticket.setInTime(inTime);
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber("ABCDEF");
        Connection con = null;
        Date outTime = null;
        boolean exist = false;
        Double price = null;
        try {
            con = dataBaseTestConfig.getConnection();
            ticketDAO.saveTicket(ticket);
        parkingSpotDAO.updateParking(parkingSpot);
        parkingService.processExitingVehicle();
            PreparedStatement ps = con.prepareStatement("select t.OUT_TIME, p.AVAILABLE, t.Price from ticket t, parking p where t.VEHICLE_REG_NUMBER ='ABCDEF' and t.PARKING_NUMBER=p.PARKING_NUMBER and p.Type ='CAR'");
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                outTime = rs.getDate(1);
                exist = rs.getBoolean(2);
                price = rs.getDouble(3);
            }
        }catch (Exception ex){
        }
            assertNotNull(outTime); // Check exit time
            assertEquals( true , exist );
            assertNotEquals(price, 0); // Verify the price
    }

    @Test
    public void testParkingLotExitRecurringUser(){
        testParkingLotExit();
        Ticket ticket = new Ticket();
        Date inTime = new Date();
        inTime.setTime( System.currentTimeMillis() - ( 60 * 60 * 1000) );
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);
        ticket.setId(2);
        ticket.setInTime(inTime);
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber("ABCDEF");
        Connection con = null;
        Date outTime = null;
        boolean exist = false;
        Double price = null;
        int nbTicket = 0;
        try {
            con = dataBaseTestConfig.getConnection();
            ticketDAO.saveTicket(ticket);
        parkingSpotDAO.updateParking(parkingSpot);
        parkingService.processExitingVehicle();
            PreparedStatement ps = con.prepareStatement("select t.OUT_TIME, p.AVAILABLE, t.Price from ticket t, parking p where t.VEHICLE_REG_NUMBER ='ABCDEF' and t.PARKING_NUMBER=p.PARKING_NUMBER and p.Type ='CAR' order by t.IN_TIME DESC limit 1 ");
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                outTime = rs.getDate(1);
                exist = rs.getBoolean(2);
                price = rs.getDouble(3);
            }
            ps = con.prepareStatement("select count(*) from ticket t, parking p where t.VEHICLE_REG_NUMBER ='ABCDEF' and t.PARKING_NUMBER=p.PARKING_NUMBER and p.Type ='CAR'");
            rs = ps.executeQuery();
            if(rs.next()){
                nbTicket = rs.getInt(1);
        }
    }catch (Exception ex){
        }
        Boolean testpass = false;
            if(price<Fare.CAR_RATE_PER_HOUR){
                //Verify if the discount was pass
                testpass = true;
            }
            //Verify if the ticketwas correctly implement
            assertNotNull(outTime);
            assertEquals( true , exist );
            assertEquals( true , testpass );
            assertNotEquals(price, 0);
            assertEquals( 2 , nbTicket );

}

@Test
public void testGetTicket(){
    Ticket ticket = new Ticket();
        Date inTime = new Date();
        Date outTime = inTime;
        inTime.setTime( System.currentTimeMillis() - ( 60 * 60 * 1000) );
        ParkingSpot parkingSpot = new ParkingSpot(1, ParkingType.CAR,false);
        ticket.setId(1);
        ticket.setInTime(inTime);
        ticket.setParkingSpot(parkingSpot);
        ticket.setVehicleRegNumber("ABCDEF");
        Double price = 1.5;
        ticket.setOutTime(outTime);
        ticket.setPrice(price);
        try {
            ticketDAO.saveTicket(ticket);
        parkingSpotDAO.updateParking(parkingSpot);
            ticketDAO.updateTicket(ticket);
}catch(Exception ex){
}
// Verify if the ticket return is correct
assertEquals(ticket.getId(), ticketDAO.getTicket("ABCDEF").getId());
assertEquals(ticket.getParkingSpot(), ticketDAO.getTicket("ABCDEF").getParkingSpot());
assertEquals(ticket.getPrice(), ticketDAO.getTicket("ABCDEF").getPrice());
assertEquals(ticket.getVehicleRegNumber(), ticketDAO.getTicket("ABCDEF").getVehicleRegNumber());
}


}
