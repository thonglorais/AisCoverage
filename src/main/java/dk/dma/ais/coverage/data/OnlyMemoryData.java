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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import dk.dma.ais.coverage.AisCoverage;
import dk.dma.ais.coverage.Helper;
import dk.dma.ais.coverage.calculator.AbstractCalculator;
import dk.dma.ais.coverage.configuration.AisCoverageConfiguration;
import dk.dma.ais.coverage.data.Ship.ShipClass;
import dk.dma.ais.coverage.data.Source.ReceiverType;
import dk.dma.ais.message.AisMessage;
import dk.dma.ais.message.AisMessage4;
import dk.dma.ais.message.AisPositionMessage;
import dk.dma.ais.packet.AisPacket;
import dk.dma.ais.packet.AisPacketTags;
import dk.dma.ais.packet.AisPacketTags.SourceType;
import dk.dma.ais.proprietary.IProprietarySourceTag;
import dk.dma.enav.model.geometry.Position;

public class OnlyMemoryData implements ICoverageData {

    private Map<Integer, Ship> ships = new ConcurrentHashMap<Integer, Ship>();
    private ConcurrentHashMap<String, Source> sources = new ConcurrentHashMap<String, Source>();
    
    public OnlyMemoryData(){
        createSource(AbstractCalculator.SUPERSOURCE_MMSI);
    }

    @Override
    public Ship getShip(int shipMmsi) {
        return ships.get(shipMmsi);
    }

    @Override
    public void updateShip(Ship ship) {

    }

    @Override
    public Cell getCell(String sourceMmsi, double lat, double lon) {
        return sources.get(sourceMmsi).getCell(lat, lon);
    }

    @Override
    public void updateCell(Cell c) {
        // TODO Auto-generated method stub

    }
    
    @Override
    public Collection<Ship> getShips(){
        return ships.values();
    }

    private List<Cell> getCells() {
        List<Cell> cells = new ArrayList<Cell>();
        Collection<Source> basestations = sources.values();
        for (Source basestation : basestations) {
            if (basestation.isVisible()) {

                // For each cell
                Collection<Cell> bscells = basestation.getGrid().values();
                for (Cell cell : bscells) {
                    cells.add(cell);
                }
            }

        }
        return cells;
    }

    @Override
    public Ship createShip(int shipMmsi, ShipClass shipClass) {
        Ship ship = new Ship(shipMmsi, shipClass);
        ships.put(shipMmsi, ship);
        return ship;
    }

    @Override
    public Cell createCell(String sourceMmsi, double lat, double lon) {
        return sources.get(sourceMmsi).createCell(lat, lon);
    }

    @Override
    public Source getSource(String sourceId) {
        return sources.get(sourceId);
    }

    @Override
    public Source createSource(String sourceId) {
        Source s = new Source(sourceId);
        sources.put(sourceId, s);
        return s;
    }

    @Override
    public Collection<Source> getSources() {
        return sources.values();
    }

    private List<Cell> getCells(double latStart, double lonStart, double latEnd, double lonEnd, Set<String> sources,
            int multiplicationFactor, Date starttime, Date endtime) {

        List<Cell> cells = new ArrayList<Cell>();

        
        for (String sourcename : sources) {
            
            //Make new cells that matches the multiplication factor
            Source source = this.sources.get(sourcename);
            if(source != null){                
                Source cellMultiplicationSource = new Source(source.getIdentifier());
                cellMultiplicationSource.setMultiplicationFactor(multiplicationFactor);
                // Make 
                Collection<Cell> bscells = source.getGrid().values();
                for (Cell cell : bscells) {

                    if (Helper.isInsideBox(cell, latStart, lonStart, latEnd, lonEnd)) {
                        Cell tempCell = cellMultiplicationSource.getCell(cell.getLatitude(), cell.getLongitude());
                        if (tempCell == null) {
                            tempCell = cellMultiplicationSource.createCell(cell.getLatitude(), cell.getLongitude());
                        }
                        tempCell.addNOofMissingSignals((int) cell.getNOofMissingSignals(starttime, endtime));
                        tempCell.addReceivedSignals(cell.getNOofReceivedSignals(starttime, endtime));
                    }

                }
                
                //add cells for particular source to cell-list.
                for (Cell cell : cellMultiplicationSource.getGrid().values()) {
                    if(cell.getNOofReceivedSignals() > 0){
                        cells.add(cell);
                    }
                }
            }
        }
        return cells;
    }

    @Override
    public List<Cell> getCells(QueryParams params) {
        if (params == null) {
            return getCells();
        }
        return getCells(params.latStart, params.lonStart, params.latEnd, params.lonEnd, params.sources,
                params.multiplicationFactor, params.startDate, params.endDate);

    }

    @Override
    public void incrementReceivedSignals(String sourceMmsi, double lat, double lon, Date timestamp) {
        Cell cell = getCell(sourceMmsi, lat, lon);
        if (cell == null) {
            cell = createCell(sourceMmsi, lat, lon);
        }
        Date id = Helper.getFloorDate(timestamp);
        TimeSpan ts = cell.getFixedWidthSpans().get(id.getTime());
        if (ts == null) {
            ts = new TimeSpan(id);
            ts.setLastMessage(Helper.getCeilDate(timestamp));
            cell.getFixedWidthSpans().put(id.getTime(), ts);
        }
        ts.setMessageCounterTerrestrial(ts.getMessageCounterTerrestrial() + 1);

    }

    @Override
    public void incrementMissingSignals(String sourceMmsi, double lat, double lon, Date timestamp) {

        Cell cell = getCell(sourceMmsi, lat, lon);
        if (cell == null) {
            cell = createCell(sourceMmsi, lat, lon);
        }
        Date id = Helper.getFloorDate(timestamp);
        TimeSpan ts = cell.getFixedWidthSpans().get(id.getTime());
        if (ts == null) {
            ts = new TimeSpan(id);
            ts.setLastMessage(Helper.getCeilDate(timestamp));
            cell.getFixedWidthSpans().put(id.getTime(), ts);
        }
        ts.incrementMissingSignals();

    }
    
    public CustomMessage packetToCustomMessage(AisPacket packet){
        
        AisMessage aisMessage = packet.tryGetAisMessage();
        if (aisMessage == null) {
            return null;
        }
        
        AisCoverageConfiguration conf = AisCoverage.get().getConf();
        String baseId = "default"; //the default id
        ReceiverType receiverType = ReceiverType.NOTDEFINED;
        Date timestamp = null;
        ShipClass shipClass = null;
        AisPositionMessage posMessage;
        SourceType sourceType = SourceType.TERRESTRIAL;

        // Get source tag properties
        IProprietarySourceTag sourceTag = aisMessage.getSourceTag();
        AisPacketTags packetTags = packet.getTags();
        
        //Determine if source is sat or terrestrial
        if(packetTags != null && packetTags.getSourceType() == SourceType.SATELLITE){
            sourceType = SourceType.SATELLITE;
        }

        //Determine source mmsi and receivertype    
        if (sourceTag != null) {
            timestamp = sourceTag.getTimestamp();
            if(sourceTag.getBaseMmsi() != null){ //it's a base station
                baseId=sourceTag.getBaseMmsi()+"";   
                receiverType = ReceiverType.BASESTATION;
            }else if(!sourceTag.getRegion().equals("")){ //It's a region
                baseId=sourceTag.getRegion();
                receiverType=ReceiverType.REGION;
            }
        }


        // If time stamp is not present, we add one
        //TODO this only makes sense for real-time data. We should check if it is real-time
        if (timestamp == null) {
            timestamp = new Date();
        }
        
        if(Helper.firstMessage == null){
            Helper.firstMessage = Helper.getFloorDate(timestamp);
        }

        // It's a base station position message
        if (aisMessage instanceof AisMessage4) {
            Source b = getSource(baseId);
            AisMessage4 m = (AisMessage4) aisMessage;
            if (conf.getSourceNameMap() != null && conf.getSourceNameMap().containsKey(baseId)) {
                //user already provided name and location for this source
            } else if (b != null) {
                Position pos = m.getPos().getGeoLocation();
                if (pos != null ) {
                    b.setLatitude(m.getPos().getGeoLocation().getLatitude());
                    b.setLongitude(m.getPos().getGeoLocation().getLongitude());
                }
            }
            return null;
        }


        // Handle position messages. If it's not a position message
        // the calculators can't use them
        if (aisMessage instanceof AisPositionMessage) {
            posMessage = (AisPositionMessage) aisMessage;
        } else {
            return null;
        }

        // Check if position is valid
        if (!posMessage.isPositionValid()) {
            return null;
        }
        
        // Extract Base station
        Source source = getSource(baseId);
        if (source == null) {
            //source wasn't found, we need to create
            source = createSource(baseId);
            source.setReceiverType(receiverType);
            
            //Let's check if user provided a name and position for this source
            if (conf.getSourceNameMap() != null && conf.getSourceNameMap().containsKey(baseId)) {
                source.setLatitude(conf.getSourceNameMap().get(baseId).getLatitude());
                source.setLongitude(conf.getSourceNameMap().get(baseId).getLongitude());   
                source.setName(conf.getSourceNameMap().get(baseId).getName());
            }
        }
        

        //Extract ship
        Ship ship = getShip(aisMessage.getUserId());
        if (ship == null) {
            ship = createShip(aisMessage.getUserId(), shipClass);
        }
        
        CustomMessage newMessage = new CustomMessage();
        newMessage.setCog((double) posMessage.getCog() / 10);
        newMessage.setSog((double) posMessage.getSog() / 10);
        newMessage.setLatitude(posMessage.getPos().getGeoLocation().getLatitude());
        newMessage.setLongitude(posMessage.getPos().getGeoLocation().getLongitude());
        newMessage.setTimestamp(timestamp);
        newMessage.addSourceMMSI(baseId);
        newMessage.setShipMMSI(aisMessage.getUserId());
        newMessage.setSourceType(sourceType);
        
        return newMessage;
    }

}
