package com.space.service;

import com.space.controller.ShipOrder;
import com.space.model.Ship;
import com.space.model.ShipType;
import com.space.repository.ShipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class ShipServiceImpl implements ShipService {

    private final ShipRepository shipRepository;

    @Autowired
    public ShipServiceImpl(ShipRepository shipRepository) {
        this.shipRepository = shipRepository;
    }

    @Override
    public Ship getShip(Long id) {
        return shipRepository.findById(id).orElse(null);
    }

    @Override
    public Ship saveShip(Ship ship) {
        return shipRepository.save(ship);
    }

    @Override
    public Ship updateShip(Ship oldShip, Ship newShip) {

        String name = newShip.getName();
        if (name != null)
            if (isStringValid(name))
                oldShip.setName(name);
            else
                throw new IllegalArgumentException();

        String planet = newShip.getPlanet();
        if (planet != null)
            if (isStringValid(planet))
                oldShip.setPlanet(planet);
            else
                throw new IllegalArgumentException();

        ShipType shipType = newShip.getShipType();
        if (shipType != null)
            oldShip.setShipType(shipType);

        Date prodDate = newShip.getProdDate();
        if (prodDate != null)
            if (isProdDateValid(prodDate))
                oldShip.setProdDate(prodDate);
            else
                throw new IllegalArgumentException();

        Boolean isUsed = newShip.getUsed();
        if (isUsed != null)
            oldShip.setUsed(isUsed);

        Double speed = newShip.getSpeed();
        if (speed != null)
            if (isSpeedValid(speed))
                oldShip.setSpeed(speed);
            else
                throw new IllegalArgumentException();

        Integer crewSize = newShip.getCrewSize();
        if (crewSize != null) {
            if (isCrewSizeValid(crewSize))
                oldShip.setCrewSize(crewSize);
            else
                throw new IllegalArgumentException();
        }

        if (speed != null || isUsed != null || prodDate != null) {
            double rating = computeRating(oldShip.getSpeed(), oldShip.getUsed(), oldShip.getProdDate());
            oldShip.setRating(rating);
        }

        shipRepository.save(oldShip);
        return oldShip;
    }

    @Override
    public void deleteShip(Ship ship) {
        shipRepository.delete(ship);
    }

    @Override
    public List<Ship> getAllShips(
            String name,
            String planet,
            ShipType shipType,
            Long after,
            Long before,
            Boolean isUsed,
            Double minSpeed,
            Double maxSpeed,
            Integer minCrewSize,
            Integer maxCrewSize,
            Double minRating,
            Double maxRating) {

        Date afterDate = after == null ? null : new Date(after);
        Date beforeDate = before == null ? null : new Date(before);

        List<Ship> list = new ArrayList<>();

        shipRepository.findAll().forEach( ship -> {
            if (name != null && !ship.getName().contains(name)) return;
            if (planet != null && !ship.getPlanet().contains(planet)) return;
            if (shipType != null && ship.getShipType() != shipType) return;
            if (afterDate != null && ship.getProdDate().before(afterDate)) return;
            if (beforeDate != null && ship.getProdDate().after(beforeDate)) return;
            if (isUsed != null && ship.getUsed().booleanValue() != isUsed.booleanValue()) return;
            if (minSpeed != null && ship.getSpeed().compareTo(minSpeed) < 0) return;
            if (maxSpeed != null && ship.getSpeed().compareTo(maxSpeed) > 0) return;
            if (minCrewSize != null && ship.getCrewSize().compareTo(minCrewSize) < 0) return;
            if (maxCrewSize != null && ship.getCrewSize().compareTo(maxCrewSize) > 0) return;
            if (minRating != null && ship.getRating().compareTo(minRating) < 0) return;
            if (maxRating != null && ship.getRating().compareTo(maxRating) > 0) return;

            list.add(ship);
            }
        );
        return list;
    }

    @Override
    public boolean isShipValid(Ship ship) {
        return ship != null &&
                isStringValid(ship.getName()) &&
                isStringValid(ship.getPlanet()) &&
                isProdDateValid(ship.getProdDate()) &&
                isSpeedValid(ship.getSpeed()) &&
                isCrewSizeValid(ship.getCrewSize());
    }

    @Override
    public double computeRating(double speed, boolean isUsed, Date prodDate) {
        final int now = 3019;
        final int prodYear = getYearFromDate(prodDate);
        final double k = isUsed ? 0.5 : 1;
        final double rating = 80 * speed * k / (now - prodYear + 1);
        return Math.round(rating * 100) / 100D;

    }

    @Override
    public List<Ship> sortShips(List<Ship> ships, ShipOrder order) {
        if (order != null) {
            ships.sort( (ship1, ship2) -> {
                switch (order) {
                    case ID:
                        return ship1.getId().compareTo(ship2.getId());
                    case DATE:
                        return ship1.getProdDate().compareTo(ship2.getProdDate());
                    case SPEED:
                        return ship1.getSpeed().compareTo(ship2.getSpeed());
                    case RATING:
                        return ship1.getRating().compareTo(ship2.getRating());
                    default:
                        return 0;
                }
            });
        }
        return ships;
    }

    @Override
    public List<Ship> getPage(List<Ship> ships, Integer pageNumber, Integer pageSize) {
        int page = pageNumber == null ? 0: pageNumber;
        int size = pageSize == null ? 3: pageSize;
        int fromPage = page * size;
        int toPage = fromPage + size;
        if (ships.size() < toPage)
            toPage = ships.size();
        return ships.subList(fromPage, toPage);
    }


    private boolean isStringValid(String value) {
        final int maxStringLength = 50;
        return value != null && !value.isEmpty() && value.length() <= maxStringLength;
    }

    private Date getDateForYear(int year) {
        final Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        return calendar.getTime();
    }

    private int getYearFromDate(Date date) {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.YEAR);
    }

    private boolean isProdDateValid(Date prodDate) {
        final Date startProd = getDateForYear(2800);
        final Date endProd = getDateForYear(3019);
        return prodDate != null && prodDate.after(startProd) && prodDate.before(endProd);
    }

    private boolean isSpeedValid(Double speed) {
        final double minSpeed = 0.01;
        final double maxSpeed = 0.99;
        return speed != null && speed.compareTo(minSpeed) >= 0 && speed.compareTo(maxSpeed) <= 0;
    }

    private boolean isCrewSizeValid(Integer crewSize) {
        final int minCrewSize = 1;
        final int maxCrewSize = 9999;
        return crewSize != null && crewSize.compareTo(minCrewSize) >= 0 && crewSize.compareTo(maxCrewSize) <= 0;
    }
}
