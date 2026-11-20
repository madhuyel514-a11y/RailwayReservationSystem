package service;

import datastructures.WaitingListQueue;
import exceptions.BookingNotFoundException;
import exceptions.SeatNotAvailableException;
import model.Booking;
import model.Passenger;
import model.Train;
import util.PNRGenerator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Handles booking, cancellation, and FIFO waiting-list promotion. */
public class ReservationService {
    private final BookingService bookingService = new BookingService();
    private final Map<Integer, WaitingListQueue<Booking>> waitingLists = new HashMap<>();
    private final Map<String, Booking> allBookingsByPnr = new HashMap<>();
    private final Set<String> issuedPnrs = new HashSet<>();

    public Booking book(Passenger passenger, Train train) {
        try {
            Booking booking = bookingService.bookConfirmed(passenger, train);
            register(booking); return booking;
        } catch (SeatNotAvailableException ex) {
            Booking waiting = new Booking(PNRGenerator.nextPnr(), passenger, train, Booking.Status.WAITING);
            register(waiting);
            waitingLists.computeIfAbsent(train.getTrainNumber(), key -> new WaitingListQueue<>()).enqueue(waiting);
            return waiting;
        }
    }
    private void register(Booking booking) { issuedPnrs.add(booking.getPnr()); allBookingsByPnr.put(booking.getPnr(), booking); }
    public Booking findBooking(String pnr) throws BookingNotFoundException {
        Booking booking = allBookingsByPnr.get(pnr);
        if (booking == null) throw new BookingNotFoundException("No booking found for PNR: " + pnr);
        return booking;
    }
    public void cancel(String pnr) throws BookingNotFoundException {
        Booking booking = findBooking(pnr);
        if (booking.getStatus() == Booking.Status.CANCELLED) throw new BookingNotFoundException("This booking is already cancelled.");
        if (booking.getStatus() == Booking.Status.WAITING) {
            booking.setStatus(Booking.Status.CANCELLED); return;
        }
        booking.setStatus(Booking.Status.CANCELLED); booking.getTrain().releaseSeat();
        WaitingListQueue<Booking> queue = waitingLists.get(booking.getTrain().getTrainNumber());
        while (queue != null && !queue.isEmpty()) {
            Booking next = queue.dequeue();
            if (next.getStatus() == Booking.Status.WAITING && booking.getTrain().reserveSeat()) {
                next.setStatus(Booking.Status.CONFIRMED); break;
            }
        }
    }
    public List<Booking> getAllBookings() { return new ArrayList<>(allBookingsByPnr.values()); }
    public WaitingListQueue<Booking> getWaitingList(int trainNumber) { return waitingLists.get(trainNumber); }
    public Set<String> getIssuedPnrs() { return new HashSet<>(issuedPnrs); }
}
