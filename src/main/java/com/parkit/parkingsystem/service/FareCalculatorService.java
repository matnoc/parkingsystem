package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket, boolean isFirstTicket){
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }
        double inHour = (double)ticket.getInTime().getTime();
        double outHour = (double)ticket.getOutTime().getTime();

        //getHours take only the hour off a Date so for 1 january 10:30 getHours give 10 and for 2 january 9:10 getHours give 9
        //With getTime we have the time in millisecond beetween our date and 1 january 1970
        double duration = (outHour - inHour)/(double)(1000*60*60);
        //With getTime if we make outHour - inHour we give the time in millisecond beetween this two date and we divid by 1000*60*60 for have this time in hour.

        if(isFirstTicket){
            if(duration <= 0.5){
                    
                    ticket.setPrice(0);
            }
            else{
                switch (ticket.getParkingSpot().getParkingType()){
                    case CAR: {
                        ticket.setPrice(duration * Fare.CAR_RATE_PER_HOUR);
                    break;
                    }
                    case BIKE: {
                        ticket.setPrice(duration * Fare.BIKE_RATE_PER_HOUR);
                    break;
                    }
                default: throw new IllegalArgumentException("Unkown Parking Type");
            }
            }
        }
        else{
            //If the vehicle have an another ticket in this parking he gain a discount
            calculateFareWithDiscount(ticket,duration);
        }
    }

    public void calculateFareWithDiscount(Ticket ticket, double duration){
        if(!(duration <= 0.5)){
        switch (ticket.getParkingSpot().getParkingType()){
        case CAR: {
              ticket.setPrice(duration * Fare.CAR_RATE_PER_HOUR * 0.95);
              break;
          }
          case BIKE: {
              ticket.setPrice(duration * Fare.BIKE_RATE_PER_HOUR * 0.95);
              break;
          }
          default: throw new IllegalArgumentException("Unkown Parking Type");
        }
    }
        else{
            ticket.setPrice(0);
        }
}
}