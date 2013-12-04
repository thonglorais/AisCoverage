/* Copyright (c) 2011 Danish Maritime Authority
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dma.ais.coverage.data;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import dk.dma.ais.coverage.data.Ship.ShipClass;

public interface ICoverageData {

    Ship createShip(String sourceMmsi, long shipMmsi, ShipClass shipClass);

    Ship getShip(String sourceMmsi, long shipMmsi);

    void updateShip(Ship ship);

    Cell createCell(String sourceMmsi, double lat, double lon);

    Cell getCell(String sourceMmsi, double lat, double lon);

    void updateCell(Cell c);

    List<Cell> getCells(QueryParams params);

    // List<Cell> getCells();
    // List<Cell> getCells(Map<String, Boolean> sources);
    // List<Cell> getCells(double latStart, double lonStart, double latEnd, double lonEnd, Map<String, Boolean> sources, int
    // multiplicationFactor);

    Source getSource(String sourceId);

    Source createSource(String sourceId);

    String[] getSourceNames();

    Collection<Source> getSources();

    void setLatSize(double latsize);

    void setLonSize(double lonsize);

    // double getLatSize();
    // double getLonSize();

    void incrementReceivedSignals(String sourceMmsi, double lat, double lon, Date timestamp);

    void incrementMissingSignals(String sourceMmsi, double lat, double lon, Date timestamp);

}