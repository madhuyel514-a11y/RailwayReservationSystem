package service;

import model.Booking;
import model.Passenger;
import model.Train;
import util.PNRGenerator;
import java.util.ArrayList;
import java.util.List;

/** Coordinates confirmed ticket bookings. */
public class BookingService {
    private final List<Booking> bookings = new ArrayList<>();
    private final SeatService seatService = new SeatService();

    public Booking bookConfirmed(Passenger passenger, Train train) {
        if (!seatService.reserveSeat(train)) return null;
        Booking booking = new Booking(PNRGenerator.nextPnr(), passenger, train, Booking.Status.CONFIRMED);
        bookings.add(booking);
        return booking;
    }
    public List<Booking> getAllBookings() { return new ArrayList<>(bookings); }
}
